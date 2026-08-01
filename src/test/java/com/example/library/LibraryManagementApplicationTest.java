package com.example.library;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class LibraryManagementApplicationTest {

    @Test
    void mainStartsTheSpringApplication() {
        String[] args = {"--spring.main.web-application-type=none"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            LibraryManagementApplication.main(args);
            springApplication.verify(() -> SpringApplication.run(LibraryManagementApplication.class, args));
        }
    }
}
