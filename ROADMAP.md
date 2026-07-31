# Roadmap and implementation approach

## 1. Scope and assumptions

The exercise prioritizes backend fundamentals and code structure rather than a
large feature set. The implementation therefore keeps the first release small:

- one library inventory
- copy counts per book rather than individually barcoded physical copies
- authenticated owner and client accounts
- a configurable loan duration supplied as 1 to 60 days
- no renewals in the first release
- removal means deactivation, not destruction of historical data

## 2. Architecture

```mermaid
flowchart LR
    A["HTTP Basic request"] --> B["Spring Security"]
    B --> C["REST controller + DTO validation"]
    C --> D["Transactional service"]
    D --> E["JPA repositories"]
    E --> F["H2 or PostgreSQL"]
    G["Flyway migrations"] --> F
    D --> H["BookReturnedEvent"]
    H --> I["After-commit listener"]
```

The package layout is feature-first (`book`, `loan`, `user`) with shared
configuration and error handling kept separate. Controllers own HTTP concerns,
services own use cases and transaction boundaries, entities protect local
invariants, and repositories own persistence access.

## 3. Delivery phases

### Phase 1 - Required foundation (implemented)

- Maven project targeting Java 25
- Spring Boot, Spring MVC, JPA, validation, and security
- Flyway schema and deterministic demo data
- searchable, paginated inventory
- owner-only add, update, and remove operations
- client borrow and return operations
- client and owner history views
- late-return tracking
- consistent validation and problem-detail responses

Completion signal: the application starts from IntelliJ with no external
services and all required workflows are callable through REST.

### Phase 2 - Correctness and confidence (implemented)

- pessimistic locking around copy-count changes
- soft deletion to preserve referential and historical integrity
- unit tests for inventory invariants
- H2-backed end-to-end API and authorization tests
- PostgreSQL Testcontainers migration test
- after-commit return event
- Docker Compose PostgreSQL environment

Completion signal: `mvn test` succeeds; PostgreSQL verification runs whenever
Docker is available.

### Phase 3 - Production hardening (recommended next)

1. Replace HTTP Basic with an external OAuth2/OIDC identity provider and JWTs.
2. Remove seeded users from the production migration and add a controlled
   provisioning workflow.
3. Add idempotency keys to borrow and return commands.
4. Add audit records for owner inventory changes.
5. Add actuator health, metrics, tracing, and structured logs.
6. Add rate limiting, production secrets management, and TLS termination.
7. Add concurrency and load tests for the last-copy borrowing scenario.
8. Publish an OpenAPI contract and generate a client SDK.

Completion signal: the service can be deployed securely, observed, and operated
without relying on demo credentials or in-memory infrastructure.

### Phase 4 - Optional library workflows

1. **Reservation queue:** create a FIFO reservation per book, prevent duplicate
   active reservations, and notify the next client after a return.
2. **Late fees:** introduce a fee policy, immutable fee registration, payment
   state, and owner reporting.
3. **Renewals:** permit renewal only when no reservation is waiting.
4. **Physical copies:** split `Book` metadata from individually tracked
   `BookCopy` records when barcode-level inventory becomes necessary.

Completion signal: queue position, fee calculations, and copy-level state are
deterministic and covered by integration tests.

### Phase 5 - Search and AI extensions

1. Enrich book metadata from a trusted ISBN provider asynchronously.
2. Add PostgreSQL full-text search or a dedicated search engine.
3. Add recommendations based on anonymized borrowing signals.
4. Expose carefully scoped read and command tools through an MCP server.
5. Build natural-language search on top of explicit, permission-checked tools.
6. Add a chat assistant API only after access control, audit, and prompt-injection
   boundaries are defined.

Completion signal: AI-facing capabilities reuse the same service-layer
authorization and cannot bypass business rules.

## 4. Data model

```text
LibraryUser 1 ----- * Loan * ----- 1 Book

LibraryUser: username, password hash, display name, role, enabled
Book: ISBN, metadata, total copies, available copies, active, version
Loan: borrower, book, borrowed at, due at, returned at, returned late
```

The loan row is the historical record. Returning a book updates that row instead
of deleting it. Books are deactivated rather than deleted, so every historical
foreign key remains valid.

## 5. Key risk controls

| Risk | Control |
|---|---|
| Two users borrow the last copy | Pessimistic lock on the book row |
| Inventory count becomes inconsistent | Entity invariants and database checks |
| Owner removes a borrowed book | Reject removal while an active copy is out |
| History disappears after removal | Soft deletion |
| Client accesses another client's loan | Ownership check in the service |
| Schema drifts from entities | Flyway migrations plus Hibernate validation |
| Event is emitted for rolled-back return | After-commit transactional listener |

## 6. Suggested review order

1. `V1__create_library_schema.sql` for the data model.
2. `BookService` and `LoanService` for business use cases.
3. `SecurityConfig` for authentication and role mapping.
4. `BookController` and `LoanController` for the REST contract.
5. `LibraryApiIntegrationTest` for executable behavior examples.
6. `PostgresContainerIntegrationTest` for database portability.

