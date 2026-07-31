package com.example.library;

import com.example.library.loan.LoanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LibraryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized());
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
    }
}
