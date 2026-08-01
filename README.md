# Library Management System

A Java 25 and Spring Boot REST API for managing a small library inventory,
borrowing books, returning them, and preserving loan history.

The project implements every required item in the exercise and two optional
items:

- PostgreSQL integration testing with Testcontainers
- An application event emitted after a book return is committed

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

## Open in IntelliJ IDEA

1. In IntelliJ, choose **File > Open**.
2. Select this `library-management-system` folder.
3. Trust the project and let IntelliJ import `pom.xml`.
4. Set the Project SDK to **Oracle JDK 25** if IntelliJ does not select it automatically.
5. Run `LibraryManagementApplication`.

The default profile uses an in-memory H2 database, applies both Flyway
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

Book search parameters:

- `query`: partial, case-insensitive title, author, or ISBN
- `availableOnly`: omit it for all active books, use `true` for books with available copies, or `false` for books with zero available copies
- `page`, `size`, and `sort`: standard Spring pagination parameters

The page size is capped at 100.

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

IntelliJ users can run the prepared requests in [requests.http](requests.http)
directly from the editor.

## Project structure

```text
src/main/java/com/example/library
|-- book/       inventory entity, repository, service, controller, DTOs
|-- loan/       loan workflow, history, return event
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
