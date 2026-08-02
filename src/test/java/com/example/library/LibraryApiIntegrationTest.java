package com.example.library;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.example.library.fee.LateFeeRepository;
import com.example.library.loan.LoanRepository;
import com.example.library.reservation.BookReservationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(LibraryApiIntegrationTest.FixedClockConfig.class)
class LibraryApiIntegrationTest {

    private static final Instant TEST_NOW = Instant.parse("2026-08-01T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LateFeeRepository lateFeeRepository;

    @Autowired
    private BookReservationRepository reservationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mcpEndpointRequiresAuthenticationAndExposesRegisteredTools() throws Exception {
        String listTools = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "tools/list",
                  "params": {}
                }
                """;

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(listTools))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/mcp")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(listTools))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools.length()").value(17))
                .andExpect(jsonPath("$.result.tools[?(@.name == 'library_search_books')]").exists())
                .andExpect(jsonPath("$.result.tools[?(@.name == 'library_add_book')]").exists());
    }

    @Test
    void mcpToolsUseTheAuthenticatedIdentityAndEnforceRoles() throws Exception {
        String searchBooks = """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "method": "tools/call",
                  "params": {
                    "name": "library_search_books",
                    "arguments": {
                      "query": "clean",
                      "availableOnly": true,
                      "page": 0,
                      "size": 5
                    }
                  }
                }
                """;
        String addBook = """
                {
                  "jsonrpc": "2.0",
                  "id": 3,
                  "method": "tools/call",
                  "params": {
                    "name": "library_add_book",
                    "arguments": {
                      "isbn": "9780321356680",
                      "title": "Effective Java",
                      "author": "Joshua Bloch",
                      "totalCopies": 1
                    }
                  }
                }
                """;
        String borrowBook = """
                {
                  "jsonrpc": "2.0",
                  "id": 4,
                  "method": "tools/call",
                  "params": {
                    "name": "library_borrow_book",
                    "arguments": {
                      "bookId": 1,
                      "loanDays": 14
                    }
                  }
                }
                """;

        mockMvc.perform(post("/mcp")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(searchBooks))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.structuredContent.content[0].title")
                        .value("Clean Code"));

        mockMvc.perform(post("/mcp")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(addBook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(true))
                .andExpect(jsonPath("$.result.content[0].text").value(
                        org.hamcrest.Matchers.containsString("Access Denied")
                ));

        mockMvc.perform(post("/mcp")
                        .with(httpBasic("owner", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(borrowBook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(true))
                .andExpect(jsonPath("$.result.content[0].text").value(
                        org.hamcrest.Matchers.containsString("Access Denied")
                ));
    }

    @Test
    void bothRolesCanSearchBooks() throws Exception {
        mockMvc.perform(get("/api/books")
                        .with(httpBasic("client", "client123"))
                        .queryParam("query", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));

        mockMvc.perform(get("/api/books")
                        .with(httpBasic("owner", "owner123"))
                        .queryParam("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void availabilityParameterFiltersAvailableAndUnavailableBooks() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 2,
                                  "loanDays": 14
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/books")
                        .with(httpBasic("client", "client123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3));

        mockMvc.perform(get("/api/books")
                        .with(httpBasic("client", "client123"))
                        .queryParam("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(get("/api/books")
                        .with(httpBasic("client", "client123"))
                        .queryParam("availableOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].available").value(false));
    }

    @Test
    void clientCannotManageInventory() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isbn": "9780321356680",
                                  "title": "Effective Java",
                                  "author": "Joshua Bloch",
                                  "totalCopies": 1
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanAddABook() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(httpBasic("owner", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isbn": "9780321356680",
                                  "title": "Effective Java",
                                  "author": "Joshua Bloch",
                                  "description": "Best practices for the Java platform.",
                                  "totalCopies": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableCopies").value(2));
    }

    @Test
    void clientCanBorrowAndReturnAnAvailableBook() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 1,
                                  "loanDays": 14
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Long loanId = loanRepository.findAll().getFirst().getId();
        assertThat(loanId).isNotNull();

        mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
                        .with(httpBasic("client", "client123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED_ON_TIME"))
                .andExpect(jsonPath("$.returnedAt").exists());

        assertThat(lateFeeRepository.count()).isZero();
    }

    @Test
    void lateReturnRegistersAClientFeeThatAnOwnerCanSettle() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 1,
                                  "loanDays": 14
                                }
                                """))
                .andExpect(status().isCreated());

        Long loanId = loanRepository.findAll().getFirst().getId();
        entityManager.flush();
        jdbcTemplate.update(
                "UPDATE loans SET borrowed_at = ?, due_at = ? WHERE id = ?",
                Timestamp.from(TEST_NOW.minusSeconds(72L * 60 * 60)),
                Timestamp.from(TEST_NOW.minusSeconds(25L * 60 * 60)),
                loanId
        );
        entityManager.clear();

        mockMvc.perform(post("/api/loans/{loanId}/return", loanId)
                        .with(httpBasic("client", "client123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED_LATE"))
                .andExpect(jsonPath("$.returnedLate").value(true));

        Long feeId = lateFeeRepository.findAll().getFirst().getId();
        assertThat(feeId).isNotNull();

        mockMvc.perform(get("/api/late-fees/my")
                        .with(httpBasic("client", "client123"))
                        .queryParam("status", "OUTSTANDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].loanId").value(loanId))
                .andExpect(jsonPath("$.content[0].daysLate").value(2))
                .andExpect(jsonPath("$.content[0].dailyRate").value(0.50))
                .andExpect(jsonPath("$.content[0].amount").value(1.00))
                .andExpect(jsonPath("$.content[0].currency").value("EUR"));

        mockMvc.perform(get("/api/late-fees/{feeId}", feeId)
                        .with(httpBasic("client", "client123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUTSTANDING"));

        mockMvc.perform(get("/api/late-fees")
                        .with(httpBasic("owner", "owner123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));

        String settlement = """
                {
                  "action": "PAID",
                  "note": "Receipt 42"
                }
                """;
        mockMvc.perform(post("/api/late-fees/{feeId}/settlement", feeId)
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settlement))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/late-fees/{feeId}/settlement", feeId)
                        .with(httpBasic("owner", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settlement))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.settlementNote").value("Receipt 42"))
                .andExpect(jsonPath("$.settledAt").exists());

        mockMvc.perform(post("/api/late-fees/{feeId}/settlement", feeId)
                        .with(httpBasic("owner", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settlement))
                .andExpect(status().isConflict());
    }

    @Test
    void fifoQueueProtectsReturnedCopyPromotesAfterExpiryAndCompletesOnBorrow() throws Exception {
        createAdditionalClient("client2", "Second Client");
        createAdditionalClient("client3", "Third Client");

        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 2,
                                  "loanDays": 14
                                }
                                """))
                .andExpect(status().isCreated());
        Long originalLoanId = loanRepository.findAll().getFirst().getId();

        String reservationRequest = """
                {
                  "bookId": 2
                }
                """;
        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client2", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.queuePosition").value(1));
        Long client2ReservationId = reservationRepository.findAll().getFirst().getId();

        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client2", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationRequest))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client3", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queuePosition").value(2));

        mockMvc.perform(get("/api/reservations/books/2/queue")
                        .with(httpBasic("owner", "owner123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].borrower.username").value("client2"))
                .andExpect(jsonPath("$.content[1].borrower.username").value("client3"));

        mockMvc.perform(get("/api/reservations/books/2/queue")
                        .with(httpBasic("client", "client123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/loans/{loanId}/return", originalLoanId)
                        .with(httpBasic("client", "client123")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reservations/{id}", client2ReservationId)
                        .with(httpBasic("client2", "client123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.expiresAt").exists());

        mockMvc.perform(get("/api/reservations/{id}", client2ReservationId)
                        .with(httpBasic("client", "client123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/books/2")
                        .with(httpBasic("owner", "owner123")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("client3", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 2,
                                  "loanDays": 14
                                }
                                """))
                .andExpect(status().isConflict());

        entityManager.flush();
        jdbcTemplate.update(
                "UPDATE book_reservations SET ready_at = ?, expires_at = ? WHERE id = ?",
                Timestamp.from(TEST_NOW.minusSeconds(100)),
                Timestamp.from(TEST_NOW),
                client2ReservationId
        );
        entityManager.clear();

        mockMvc.perform(post("/api/loans")
                        .with(httpBasic("client3", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 2,
                                  "loanDays": 14
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reservations/my")
                        .with(httpBasic("client2", "client123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("EXPIRED"));
        mockMvc.perform(get("/api/reservations/my")
                        .with(httpBasic("client3", "client123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("FULFILLED"));

        mockMvc.perform(get("/api/reservations")
                        .with(httpBasic("owner", "owner123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("owner", "owner123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationRequest))
                .andExpect(status().isForbidden());
    }

    private void createAdditionalClient(String username, String displayName) {
        jdbcTemplate.update(
                """
                        INSERT INTO library_users (
                            username,
                            password_hash,
                            display_name,
                            role,
                            enabled
                        )
                        SELECT ?, password_hash, ?, 'CLIENT', TRUE
                        FROM library_users
                        WHERE username = 'client'
                        """,
                username,
                displayName
        );
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(TEST_NOW, ZoneOffset.UTC);
        }
    }
}
