package com.example.library;

import com.example.library.book.BookRepository;
import com.example.library.fee.LateFeeRepository;
import com.example.library.reservation.BookReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresContainerIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("library")
            .withUsername("library")
            .withPassword("library");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LateFeeRepository lateFeeRepository;

    @Autowired
    private BookReservationRepository reservationRepository;

    @Test
    void flywayCreatesAndSeedsThePostgresDatabase() {
        Integer books = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM books", Integer.class);
        Integer migrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
                Integer.class
        );

        assertThat(books).isEqualTo(3);
        assertThat(migrations).isEqualTo(4);
        assertThat(lateFeeRepository.count()).isZero();
        assertThat(reservationRepository.count()).isZero();
        jdbcTemplate.update("UPDATE books SET available_copies = 0 WHERE id = 2");

        assertThat(bookRepository.search("", -1, PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(3);
        assertThat(bookRepository.search("", 1, PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(2);
        assertThat(bookRepository.search("", 0, PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(1);
    }
}
