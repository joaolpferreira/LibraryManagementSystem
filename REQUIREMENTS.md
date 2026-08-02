# Requirements traceability

This matrix is the review checklist for `Library_Management_System.pdf`. Every
required and optional item is linked to its primary implementation and executable
evidence. The full build command is `./mvnw verify` (`.\mvnw.cmd verify` on
Windows); it runs unit/API integration tests, Flyway against PostgreSQL 18 in
Testcontainers when Docker is available, and the 100% line/branch JaCoCo gate.

## Core functionalities

| # | Requirement | Primary implementation | Executable evidence |
|---|---|---|---|
| 1 | Both roles view and search books | `BookController`, `BookService`, `BookRepository.search` | `BookServiceTest`, `LibraryApiIntegrationTest` |
| 2 | Clients borrow an available book | `LoanController.borrow`, `LoanService.borrow`; pessimistic book lock | `LoanServiceTest`, `LibraryApiIntegrationTest` |
| 3 | Clients return a book | `LoanController.returnBook`, `LoanService.returnBook` | `LoanServiceTest`, `LibraryApiIntegrationTest` |
| 4 | Owners add, update, and remove books | owner-guarded `BookController` commands; history-safe soft delete | `BookServiceTest`, `LibraryApiIntegrationTest` |
| 5 | Borrower, return, and late history | persisted `Loan` timestamps/status; `/api/loans/my` and `/api/loans/history` | `LoanTest`, `LibraryApiIntegrationTest` |
| 6 | Client/owner authentication | database users, BCrypt, HTTP Basic, request and method authorization | `SecurityConfigTest`, `LibraryApiIntegrationTest` |

## Technical requirements

| # | Requirement | Implementation | Verification |
|---|---|---|---|
| 1 | Java 25 | compiler release 25 plus Maven Enforcer range `[25,26)` | Maven `validate` phase |
| 2 | Maven | Maven Wrapper 3.3.4 using Maven 3.9.16 | `mvnw --version`; Enforcer `[3.9,4.0)` |
| 3 | Spring Boot | Spring Boot 4.1.0 parent and starters | application context tests |
| 4 | Spring Data JPA or JDBC | JPA entities/repositories; JDBC used for focused integration fixtures | unit and H2/PostgreSQL integration tests |
| 5 | REST APIs | 25 secured Spring MVC operations with RFC 9457 errors | `openapi.yml`, controller and API integration tests |
| 6 | Flyway | five immutable versioned migrations; Hibernate `ddl-auto: validate` | H2 and PostgreSQL startup tests |

## Optional tasks

| # | Requirement | Primary implementation | Executable evidence / interface |
|---|---|---|---|
| 1 | Testcontainers with JUnit | `PostgresContainerIntegrationTest`, PostgreSQL 18 container | `mvnw verify` with Docker running |
| 2 | Emit event on return | `BookReturnedEvent`, after-commit listener | `BookReturnedEventListenerTest`, application logs |
| 3 | Queue for a book | transactional FIFO `ReservationService`, protected READY allocation and expiry job | reservation unit/API tests; `/api/reservations` |
| 4 | Late fee registration | atomic fee policy/record on overdue return; owner settlement audit | fee unit/API tests; `/api/late-fees` |
| 5 | MCP server for AI agents | authenticated stateless Spring AI MCP endpoint with 21 role-checked tools | MCP API integration tests; `/mcp`; `mcp-requests.http` |
| 6 | Natural-language search | deterministic English/Portuguese parser with tri-state availability | parser/API tests; `/api/search/books/natural` |
| 7 | Recommendation system | per-client, explainable scoring from history, subjects, popularity, availability | recommendation tests; `/api/recommendations/my` |
| 8 | Metadata enrichment | bounded Open Library ISBN snapshots; external call outside transaction; locked ISBN recheck | metadata tests; `/api/books/{id}/metadata/enrich` |
| 9 | Chat assistant API | grounded read-only intent router over authenticated application services | assistant tests; `/api/assistant/chat` |

## Review artifacts

- `openapi.yml`: complete REST contract for Swagger/IntelliJ.
- `postman/Library_Management_System_All_Features.postman_collection.json`:
  runnable REST examples grouped by core and optional capabilities.
- `mcp-requests.http`: MCP initialization, tool discovery, and tool call examples.
- `target/site/jacoco/index.html`: generated coverage report after `mvnw verify`.
