# Library Management System

A Java 25 and Spring Boot REST API for managing a small library inventory,
borrowing books, returning them, and preserving loan history.

The project implements every required item in the exercise and four optional
items:

- PostgreSQL integration testing with Testcontainers
- An application event emitted after a book return is committed
- automatic, auditable late-fee registration and settlement
- transactional FIFO reservation queues with expiring copy allocations

See [ROADMAP.md](ROADMAP.md) for the implementation plan, design decisions, and
suggested next phases.

## Requirements covered

| Exercise requirement | Implementation |
|---|---|
| Client and owner roles | Database-backed users and Spring Security HTTP Basic |
| View and search books | Paginated `GET /api/books` with text and availability filters |
| Borrow available books | Transactional `POST /api/loans` |
| Return books | Transactional `POST /api/loans/{id}/return` |
| Owner inventory management | Create, update, and soft-delete endpoints |
| Borrowing history | Client-specific and owner-wide history endpoints |
| Late tracking | Due date, return date, stored `returnedLate`, and calculated status |
| Late-fee registration | Configurable calculation, immutable fee records, visibility, and settlement |
| Reservation queue | FIFO waiting, ready holds, cancellation, expiry, and fair borrowing |
| Java 25 and Maven | Compiler release 25 and Maven Wrapper |
| Spring Data JPA | JPA entities and repositories |
| REST APIs | Spring MVC controllers with validated DTOs |
| Database migrations | Flyway versioned SQL migrations |

## Technical choices

- **Spring Boot 4.1.0** with Java 25
- **H2** in PostgreSQL compatibility mode for zero-setup local development
- **PostgreSQL 18** through Docker Compose for a production-like run
- **Flyway** owns the schema; Hibernate runs with `ddl-auto: validate`
- **Pessimistic book locking** prevents two clients from borrowing the last copy
- **Soft deletion** hides removed books while preserving historical loan records
- **RFC 9457 problem details** provide consistent API errors
- **BCrypt** protects the seeded demo passwords
- **Atomic late-fee registration** keeps returns and financial records consistent
- **Reservation-aware borrowing** prevents clients bypassing allocated copies

## Open in IntelliJ IDEA

1. In IntelliJ, choose **File > Open**.
2. Select this `library-management-system` folder.
3. Trust the project and let IntelliJ import `pom.xml`.
4. Set the Project SDK to **Oracle JDK 25** if IntelliJ does not select it automatically.
5. Run `LibraryManagementApplication`.

The default profile uses an in-memory H2 database, applies all Flyway
migrations, and starts on `http://localhost:8080`.

Demo accounts:

| Role | Username | Password |
|---|---|---|
| Owner | `owner` | `owner123` |
| Client | `client` | `client123` |

The credentials are for local demonstration only.

## Run from a terminal

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS or Linux:

```bash
./mvnw spring-boot:run
```

Run all tests:

```powershell
.\mvnw.cmd test
```

The Testcontainers test runs when Docker is available and is skipped otherwise.

## Code coverage

Run the tests and generate the JaCoCo HTML and XML reports:

```powershell
.\mvnw.cmd clean verify
```

Open `target/site/jacoco/index.html` to inspect coverage by package, class,
method, and line. SonarQube automatically imports the XML report from
`target/site/jacoco/jacoco.xml`. The Maven `verify` phase enforces 100% line and
branch coverage, so a coverage regression fails the build.

## SonarQube

Start the local SonarQube Community Build and its dedicated PostgreSQL database:

```powershell
docker compose --profile sonar up -d sonar-db sonarqube
```

Open `http://localhost:9000`. On a new installation, sign in with `admin` / `admin`,
change the initial password, and generate a user token under **My Account > Security**.

Run tests, regenerate coverage, and submit the analysis:

```powershell
$env:SONAR_TOKEN = "your-token"
.\mvnw.cmd clean verify sonar:sonar `
  "-Dsonar.host.url=http://localhost:9000" `
  "-Dsonar.token=$env:SONAR_TOKEN"
```

Stop SonarQube without deleting its persisted data:

```powershell
docker compose --profile sonar stop sonarqube sonar-db
```

## Run with PostgreSQL

Start PostgreSQL:

```powershell
docker compose up -d
```

Run the application with the PostgreSQL profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

The profile accepts these optional environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Stop the container:

```powershell
docker compose down
```

Add `-v` only if you intentionally want to delete the local database volume.

## API

All endpoints require HTTP Basic authentication.

The complete, importable API contract is available in [`openapi.yml`](openapi.yml).
Open that file with the IntelliJ Swagger/OpenAPI preview, the VS Code Swagger
Viewer extension, or import it into Swagger Editor. The contract documents all
20 current operations, role requirements, query parameters, paging, request and
response schemas, and RFC 9457 error responses.

| Method | Path | Role | Purpose |
|---|---|---|---|
| `GET` | `/api/books` | Client, Owner | Search active books |
| `GET` | `/api/books/{id}` | Client, Owner | Get one active book |
| `POST` | `/api/books` | Owner | Add a book |
| `PUT` | `/api/books/{id}` | Owner | Update book details and copy count |
| `DELETE` | `/api/books/{id}` | Owner | Soft-delete a book with no active loans |
| `POST` | `/api/loans` | Client | Borrow an available book |
| `GET` | `/api/loans/{id}` | Owner or owning client | Get one loan |
| `POST` | `/api/loans/{id}/return` | Owning client | Return a book |
| `GET` | `/api/loans/my` | Client | View personal loan history |
| `GET` | `/api/loans/history` | Owner | View all loan history |
| `GET` | `/api/late-fees/my` | Client | View personal late fees |
| `GET` | `/api/late-fees` | Owner | View and filter all late fees |
| `GET` | `/api/late-fees/{feeId}` | Owner or owning client | Get one late fee |
| `POST` | `/api/late-fees/{feeId}/settlement` | Owner | Record a fee as paid or waived |
| `POST` | `/api/reservations` | Client | Join an unavailable book's FIFO queue |
| `GET` | `/api/reservations/my` | Client | View personal reservation history |
| `GET` | `/api/reservations` | Owner | View and filter all reservations |
| `GET` | `/api/reservations/books/{bookId}/queue` | Owner | Inspect a book's active queue |
| `GET` | `/api/reservations/{reservationId}` | Owner or owning client | Get one reservation |
| `POST` | `/api/reservations/{reservationId}/cancel` | Owning client | Cancel an active reservation |

Book search parameters:

- `query`: partial, case-insensitive title, author, or ISBN
- `availableOnly`: omit it for all active books, use `true` for books with available copies, or `false` for books with zero available copies
- `page`, `size`, and `sort`: standard Spring pagination parameters

The page size is capped at 100.

Late fees use the `library.late-fee.daily-rate` and
`library.late-fee.currency` settings in `application.yml`. The default policy
charges EUR 0.50 for every started 24-hour period after the exact due time. A
late fee snapshots its calculation permanently, so later rate changes affect
only future returns. Settlement records an external payment or owner-approved
waiver; the API does not process card or bank payments.

Reservations use a strict FIFO queue. When a copy becomes available, the oldest
waiting reservation becomes `READY` and owns that copy for 48 hours by default.
Other clients cannot borrow an allocated copy. Expired holds are reconciled by a
scheduled job and also during queue-sensitive operations. Configure the hold and
scan interval with `library.reservation.ready-hold` and
`library.reservation.expiration-scan-delay`.

## Quick API walkthrough

List books:

```powershell
curl.exe -u client:client123 "http://localhost:8080/api/books?query=clean"
```

Add a book as the owner:

```powershell
curl.exe -u owner:owner123 `
  -H "Content-Type: application/json" `
  -d '{"isbn":"9780321356680","title":"Effective Java","author":"Joshua Bloch","totalCopies":2}' `
  http://localhost:8080/api/books
```

Borrow seeded book `1`:

```powershell
curl.exe -u client:client123 `
  -H "Content-Type: application/json" `
  -d '{"bookId":1,"loanDays":14}' `
  http://localhost:8080/api/loans
```

Return loan `1`:

```powershell
curl.exe -u client:client123 `
  -X POST `
  http://localhost:8080/api/loans/1/return
```

View the client's outstanding late fees:

```powershell
curl.exe -u client:client123 `
  "http://localhost:8080/api/late-fees/my?status=OUTSTANDING"
```

Record late fee `1` as paid:

```powershell
curl.exe -u owner:owner123 `
  -H "Content-Type: application/json" `
  -d '{"action":"PAID","note":"Receipt 42"}' `
  http://localhost:8080/api/late-fees/1/settlement
```

Join the queue for unavailable book `2`:

```powershell
curl.exe -u client:client123 `
  -H "Content-Type: application/json" `
  -d '{"bookId":2}' `
  http://localhost:8080/api/reservations
```

View personal reservations:

```powershell
curl.exe -u client:client123 http://localhost:8080/api/reservations/my
```

IntelliJ users can run the prepared requests in [requests.http](requests.http)
directly from the editor.

## Project structure

```text
src/main/java/com/example/library
|-- book/       inventory entity, repository, service, controller, DTOs
|-- loan/       loan workflow, history, return event
|-- fee/        late-fee policy, registration, queries, settlement, event
|-- reservation/ FIFO queues, copy allocation, expiry job, controller, event
|-- user/       database user and roles
|-- config/     security, clock, stable page serialization
`-- common/     problem-detail exception handling

src/main/resources
|-- db/migration/              Flyway schema and seed data
|-- application.yml            zero-setup H2 profile
`-- application-postgres.yml   PostgreSQL profile
```

## Important business rules

- Only clients borrow and return books.
- A client cannot hold two active loans for the same book.
- A borrow fails when no copy is available.
- A client can return only their own active loan.
- Reducing total copies below the active-loan count is rejected.
- A book with active loans cannot be removed.
- Removal is soft deletion so history remains readable.
- Late status is decided using UTC when the return is recorded.
- A late return atomically creates one immutable fee per loan.
- Only owners can record a late fee as paid or waived.
- Reservations are allowed only when no unallocated copy can be borrowed.
- One client can hold only one active reservation for a given book.
- Returned copies are allocated FIFO and cannot be taken by another client.
- Ready allocations expire after the configured hold unless borrowed or cancelled.
