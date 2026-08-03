# Library Management System

A Java 25 and Spring Boot REST API for managing a small library inventory,
borrowing books, returning them, and preserving loan history.

The project provides the complete library workflow plus its search and
AI-integration extensions:

- PostgreSQL integration testing with Testcontainers
- An application event emitted after a book return is committed
- automatic, auditable late-fee registration and settlement
- transactional FIFO reservation queues with expiring copy allocations
- a secured MCP server that exposes the business use cases to AI agents
- English/Portuguese natural-language catalog search
- explainable recommendations from borrowing and catalog signals
- persisted ISBN metadata enrichment through Open Library
- a read-only, role-aware chat assistant API grounded in live application data

## Implemented capabilities

| Capability | Implementation |
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
| AI-agent integration | Authenticated MCP tools over stateless Streamable HTTP |
| Natural-language search | Transparent English/Portuguese intent parsing over the existing catalog query |
| Recommendation system | Explainable ranking from history, subjects, popularity, and availability |
| Metadata enrichment | Versioned Open Library snapshots persisted by ISBN |
| Chat assistant API | Read-only role-aware intent routing to live services |
| Java 25 and Maven | Compiler release 25 and Maven Wrapper |
| Spring Data JPA | JPA entities and repositories |
| REST APIs | Spring MVC controllers with validated DTOs |
| Database migrations | Flyway versioned SQL migrations |

## Architecture

![Library Management System architecture](docs/architecture.svg)

REST controllers and MCP tools are inbound adapters over the same application
services. This keeps authorization and transport concerns at the boundary while
preserving one implementation of the inventory, loan, return, fee, reservation,
search, recommendation, metadata, and assistant rules. Spring Data repositories
and Flyway own persistence; PostgreSQL is used by the Docker profile and H2 keeps
the default development profile self-contained.

## Technical choices

- **Spring Boot 4.1.0** with Java 25
- **Maven Enforcer** fails fast unless the wrapper is running on Java 25 and Maven 3.9
- **H2** in PostgreSQL compatibility mode for zero-setup local development
- **PostgreSQL 18** through Docker Compose for a production-like run
- **Flyway** owns the schema; Hibernate runs with `ddl-auto: validate`
- **Pessimistic book locking** prevents two clients from borrowing the last copy
- **Soft deletion** hides removed books while preserving historical loan records
- **RFC 9457 problem details** provide consistent API errors
- **BCrypt** protects the seeded demo passwords
- **Atomic late-fee registration** keeps returns and financial records consistent
- **Reservation-aware borrowing** prevents clients bypassing allocated copies
- **Spring AI MCP 2.0** exposes explicit, permission-checked tools at `/mcp`
- **Open Library Search API** supplies optional ISBN metadata without an API key
- **Grounded assistant orchestration** returns only database-backed results and performs no mutation

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

## MCP server for AI agents

The same Spring Boot process exposes a Model Context Protocol server at
`http://localhost:8080/mcp`. It uses stateless Streamable HTTP and Spring AI
2.0.0. MCP requests use the same HTTP Basic users as the REST API, and every
tool has method-level role checks before it reaches the existing transactional
services. An AI agent therefore cannot bypass loan, inventory, fee, reservation,
ownership, or concurrency rules.

The server publishes 21 tools:

| Access | Tools |
|---|---|
| Client or owner | `library_search_books`, `library_get_book`, `library_natural_language_search`, `library_get_book_metadata` |
| Client | `library_borrow_book`, `library_return_book`, `library_get_my_loans`, `library_reserve_book`, `library_get_my_reservations`, `library_cancel_reservation`, `library_get_my_late_fees`, `library_get_recommendations` |
| Owner | `library_add_book`, `library_update_book`, `library_remove_book`, `library_get_loan_history`, `library_get_reservation_history`, `library_get_book_queue`, `library_get_late_fee_history`, `library_settle_late_fee`, `library_enrich_book_metadata` |

Tool parameters have generated JSON Schemas, paging is bounded to 100 results,
mutating tools reuse validated request DTOs, and MCP annotations tell agents
which tools are read-only or destructive. Client-specific tools obtain the
username from the authenticated security context; they never accept an
impersonation username from the model.

Use the official MCP Inspector to view schemas and call tools interactively as
the demo client:

```powershell
npx -y @modelcontextprotocol/inspector `
  --server-url "http://localhost:8080/mcp" `
  --transport http `
  --header "Authorization: Basic Y2xpZW50OmNsaWVudDEyMw=="
```

The command opens a read-only Inspector session with the server URL and header
already supplied. A read-only-session banner is expected: it means only that
the launch configuration cannot be edited in the UI, not that MCP tools are
read-only. To connect as the owner instead, replace the header value with:

```text
Authorization: Basic b3duZXI6b3duZXIxMjM=
```

The client value authenticates `client / client123`; the owner value
authenticates `owner / owner123`. Use only one identity per connection. The demo
credentials must be replaced before any non-local deployment.

List the tools directly from a terminal:

```powershell
npx -y @modelcontextprotocol/inspector --cli http://localhost:8080/mcp `
  --transport http `
  --method tools/list `
  --header "Authorization: Basic Y2xpZW50OmNsaWVudDEyMw=="
```

Call the book-search tool:

```powershell
npx -y @modelcontextprotocol/inspector --cli http://localhost:8080/mcp `
  --transport http `
  --method tools/call `
  --tool-name library_search_books `
  --tool-arg query=clean `
  --tool-arg availableOnly=true `
  --tool-arg page=0 `
  --tool-arg size=20 `
  --header "Authorization: Basic Y2xpZW50OmNsaWVudDEyMw=="
```

`mcp-requests.http` contains equivalent JSON-RPC requests for IntelliJ. MCP is
not added to `openapi.yml` because OpenAPI describes the REST API, while `/mcp`
uses the MCP JSON-RPC protocol and advertises its own generated tool schemas.

## API

All endpoints require HTTP Basic authentication.
There is no separate login endpoint: send the Basic credentials on every REST
request. Resource-specific reads additionally enforce ownership for clients.

The complete, importable API contract is available in [`openapi.yml`](openapi.yml).
Open that file with the IntelliJ Swagger/OpenAPI preview, the VS Code Swagger
Viewer extension, or import it into Swagger Editor. The contract documents all
25 current operations, role requirements, query parameters, paging, request and
response schemas, and RFC 9457 error responses.

For Postman Desktop, import
[`postman/Library_Management_System_All_Features.postman_collection.json`](postman/Library_Management_System_All_Features.postman_collection.json).
It contains all REST operations, collection variables, authentication, example
bodies, and folders matching the core and optional capabilities.

| Method | Path | Access | Main input | Success | Purpose |
|---|---|---|---|---|---|
| `GET` | `/api/books` | Client, Owner | `query`, `availableOnly`, `page`, `size`, `sort` | `200` | Search and filter active books |
| `GET` | `/api/books/{id}` | Client, Owner | Book ID | `200` | Get one active book |
| `POST` | `/api/books` | Owner | `BookRequest` JSON | `201` | Add a book |
| `PUT` | `/api/books/{id}` | Owner | Book ID and `BookRequest` JSON | `200` | Replace editable book details and copy count |
| `DELETE` | `/api/books/{id}` | Owner | Book ID | `204` | Soft-delete a book with no active loans or reservations |
| `POST` | `/api/loans` | Client | `BorrowBookRequest` JSON | `201` | Borrow an available, unallocated copy |
| `GET` | `/api/loans/{loanId}` | Owner or owning client | Loan ID | `200` | Get one loan |
| `POST` | `/api/loans/{loanId}/return` | Owning client | Loan ID | `200` | Return a book, register any fee, and advance its queue |
| `GET` | `/api/loans/my` | Client | `page`, `size`, `sort` | `200` | View personal loan history |
| `GET` | `/api/loans/history` | Owner | `page`, `size`, `sort` | `200` | View complete loan history |
| `GET` | `/api/late-fees/my` | Client | `status`, `page`, `size` | `200` | View personal late-fee history |
| `GET` | `/api/late-fees` | Owner | `status`, `page`, `size` | `200` | View and filter every late fee |
| `GET` | `/api/late-fees/{feeId}` | Owner or owning client | Late-fee ID | `200` | Get one late fee |
| `POST` | `/api/late-fees/{feeId}/settlement` | Owner | Fee ID and `LateFeeSettlementRequest` JSON | `200` | Record a fee as paid or waived |
| `POST` | `/api/reservations` | Client | `ReserveBookRequest` JSON | `201` | Join an unavailable book's FIFO queue |
| `GET` | `/api/reservations/my` | Client | `status`, `page`, `size` | `200` | View personal reservation history |
| `GET` | `/api/reservations` | Owner | `status`, `page`, `size` | `200` | View and filter every reservation |
| `GET` | `/api/reservations/books/{bookId}/queue` | Owner | Book ID, `page`, `size` | `200` | Inspect a book's active FIFO queue |
| `GET` | `/api/reservations/{reservationId}` | Owner or owning client | Reservation ID | `200` | Get one reservation |
| `POST` | `/api/reservations/{reservationId}/cancel` | Owning client | Reservation ID | `200` | Cancel a waiting or ready reservation |
| `GET` | `/api/search/books/natural` | Client, Owner | `question`, `page`, `size`, `sort` | `200` | Interpret and execute an English/Portuguese catalog question |
| `GET` | `/api/recommendations/my` | Client | `limit` | `200` | Get explainable personalized recommendations |
| `GET` | `/api/books/{bookId}/metadata` | Client, Owner | Book ID | `200` | Read persisted enriched metadata |
| `POST` | `/api/books/{bookId}/metadata/enrich` | Owner | Book ID | `200` | Refresh metadata from Open Library by ISBN |
| `POST` | `/api/assistant/chat` | Client, Owner | `AssistantRequest` JSON | `200` | Ask a read-only, grounded library question |

These are all 25 REST operations implemented by the controllers. The separate
`POST /mcp` transport endpoint is documented in the MCP section because its
requests are MCP JSON-RPC messages rather than REST resources.

## Search, recommendations, metadata, and assistant

Natural-language search is deliberately transparent: the response includes the
catalog text and tri-state availability filter extracted from the question.
Examples include `Find available books by Martin Fowler`, `livros indisponíveis`,
and a plain title or ISBN.

Recommendations never expose another client's history. Previously borrowed
books are excluded, and each result explains the signals that contributed to
its score. Enriched subjects improve personalization, but the system also has a
cold-start ranking based on popularity and availability.

Metadata enrichment is an explicit OWNER operation. It requests a small set of
fields from Open Library by ISBN, caps subjects at 20, stores a versioned snapshot
through Flyway-managed tables, and returns `502 Bad Gateway` if the provider is
temporarily unavailable. Existing core title/author/inventory data is never
overwritten by the external source. Provider values are normalized to database
limits. The network request runs outside a database transaction; a short
pessimistically locked transaction then rechecks the ISBN before persisting, so
a concurrent owner edit cannot attach stale metadata to the wrong ISBN.

The chat endpoint is usable without an LLM key. It recognizes help, catalog
search, recommendations, loans, reservations, and late-fee questions in English
or Portuguese, delegates to the same services as REST/MCP, and returns structured
evidence. It is read-only by design; borrow, return, settlement, enrichment, and
inventory changes remain explicit authenticated commands.

Book search parameters:

- `query`: partial, case-insensitive title, author, ISBN, description, or enriched subject
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
|-- mcp/        secured AI-agent tools, input validation, paged responses
|-- search/     transparent English/Portuguese natural-language catalog parsing
|-- recommendation/ explainable, authenticated recommendation ranking
|-- metadata/   Open Library adapter and concurrency-safe snapshot persistence
|-- assistant/  deterministic, read-only chat intent orchestration
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
