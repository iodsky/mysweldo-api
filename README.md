# mysweldo-api

A production-grade payroll management REST API built with Spring Boot. It models Philippine statutory payroll rules (SSS, PhilHealth, Pag-IBIG, and withholding tax) alongside employee records, attendance, overtime, leave, and full payroll runs.

This project serves as a demonstration of solid backend engineering: layered domain architecture, stateless JWT security, database migrations, async CSV imports, and a clean CI/CD + container deployment pipeline to AWS EC2.

> Live: `https://mysweldo-api.iodsky.com` · Swagger UI: `https://mysweldo-api.iodsky.com/api/swagger-ui.html`

## Tech Stack

| Concern | Technology |
| --- | --- |
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Build | Gradle (Kotlin DSL, version catalog) |
| Persistence | Spring Data JPA + PostgreSQL 16 |
| Migrations | Flyway |
| Security | Spring Security + JWT (access + refresh, HTTP-only cookies) |
| Batch | OpenCSV (async CSV imports) |
| API Docs | springdoc-openapi (Swagger UI) |
| Container | Docker (multi-stage, `eclipse-temurin`) |

## Features

- **Employee management** — departments, positions, employment types, pay types, salaries, government IDs.
- **Time & attendance** — clock-in/clock-out, attendance views, overtime requests.
- **Leave management** — leave credits, leave requests with approval flow.
- **Statutory tables** — SSS, PhilHealth, Pag-IBIG contribution tables and tax brackets.
- **Payroll engine** — pay-basis strategies (hourly/daily/monthly), statutory deduction & contribution computation, payroll runs.
- **Security** — role-based access control, stateless JWT auth with refresh tokens.
- **Bulk import** — on-demand OpenCSV imports for CSV uploads (employees & users).
- **Standardized responses** — `ApiResponse` envelope with pagination metadata and centralized exception handling.
- **Soft delete & auditing** — every entity extends a common `BaseModel` (timestamps, optimistic locking, soft delete).

## Architecture

The codebase follows a **strict layered architecture**, with each business domain owning the same vertical slice:

```
<Domain>Controller.java   # REST endpoints (returns ResponseFactory envelopes)
<Domain>Service.java      # business logic (@Service, @RequiredArgsConstructor)
<Domain>Repository.java   # Spring Data JPA
<Domain>Entity.java       # JPA entity extends BaseModel
<Domain>Request.java      # inbound DTO
<Domain>Dto.java          # outbound DTO
<Domain>Mapper.java       # Entity <-> DTO conversion (also Request -> Entity)
```

**Domains:** `attendance`, `benefit`, `contribution`, `deduction`, `department`, `employee`, `leave`, `overtime`, `pagIbig`, `philhealth`, `position`, `security`, `sss`, `tax`, and the `payroll` engine.

### Shared infrastructure (`common/`)

- `BaseModel` — superclass of every entity; provides `createdAt`/`updatedAt`, `createdBy`/`lastModifiedBy` (JPA auditing), `version` (optimistic locking), and `deletedAt` (soft delete via `@SQLRestriction`).
- `response/` — `ResponseFactory` builds a consistent `ApiResponse` envelope; errors flow through `GlobalExceptionHandler`.
- Services signal failures with `ResponseStatusException`.

### Payroll engine (`payroll/`)

Payroll logic is deliberately decomposed, not centralized:

- `payroll/core` — `PayrollCalculator` + `PayrollItemAssembler` compose statutory deductions/contributions and line items.
- `payroll/strategy` — `PayBasisStrategyFactory` selects the hourly/daily/monthly pay-basis strategy; `StandardPayrollComputationStrategy` orchestrates computation.
- `payroll/run` — `PayrollRunService` executes full payroll runs across active employees.

### Security

- JWT issued at `/auth/login` and `/auth/refresh`; refresh tokens are delivered as HTTP-only cookies.
- Only `/auth/**`, `/docs/**`, and `/swagger-ui/**` are public; everything else requires a valid Bearer token.
- Role-based access is enforced via Spring method security (`@EnableMethodSecurity`).

### Imports / uploads

- CSV imports use OpenCSV via `imports/ImportController`: `POST /jobs/import-employees`, `POST /jobs/import-users` (async) and `GET /jobs/{id}` for status + per-row failures.
- CSV uploads are capped at 10 MB and stored under `uploads/`.

## Getting Started

### Prerequisites

- Java 21
- Docker (for the local PostgreSQL + pgAdmin)
- `.env` file (see below)

### 1. Configure environment

```bash
cp .env.template .env
```

Fill in the DB credentials, JWT secret, and expiration values. `.env` is loaded through `spring.config.import`, not Gradle — any new environment variable must also be added to `.env.template`.

### 2. Start the local database

```bash
docker compose -f db.compose.yml up -d
```

This starts PostgreSQL 16 on `localhost:5432` and pgAdmin at `:5050`.

### 3. Run the application

```bash
./run.sh
```

`run.sh` sources `.env` and runs `./gradlew bootRun` with the `local` profile. The server listens on the port defined by `PORT` (default `8001`) under the `/api` context path.

### 4. Explore the API

- Swagger UI: `http://localhost:8001/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8001/api/docs`

## Testing

```bash
# All tests
./gradlew clean test

# Single test class / method
./gradlew test --tests "com.iodsky.mysweldo.benefit.BenefitServiceTest"
./gradlew test --tests "com.iodsky.mysweldo.benefit.BenefitServiceTest.methodName"
```

Tests use JUnit 5 + AssertJ, are named `<Domain>ServiceTest`, and live under `src/test/java/com/iodsky/mysweldo/<domain>/`. Repository/view stubs are colocated with the tests.

## Database

- Flyway migrations live in `src/main/resources/db/migration/` as `V{n}__description.sql`.
- **Never modify an existing migration** — both profiles run `ddl-auto: validate` and Flyway checksums will fail. Add a new `V{n+1}__...sql` instead.

## Configuration

| Profile | Activation | Database |
| --- | --- | --- |
| `local` | default via `./run.sh` | `localhost:5432` from `.env` (`LOCAL_DB_*`) |
| `prod`  | set at CI/CD deploy | Cloud PostgreSQL from `.env` (`CLOUD_DB_*`) |

Key environment variables (see `.env.template`): `PORT`, `LOCAL_DB_*`, `CLOUD_DB_*`, `JWT_SECRET_KEY`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `JWT_COOKIE_*`, `CORS_ALLOWED_ORIGINS`.

## Deployment (AWS EC2)

The API is containerized and deployed to an EC2 instance, exposed at `https://mysweldo-api.iodsky.com` through a Traefik reverse proxy with automatic Let's Encrypt TLS.

### Build & publish the image

The CI pipeline builds a multi-stage Docker image and pushes it to GitHub Container Registry:

```bash
./gradlew clean bootJar
docker build -t ghcr.io/iodsky/mysweldo-api:latest .
docker push ghcr.io/iodsky/mysweldo-api:latest
```

### Run on EC2

```bash
docker compose -f mysweldo-api.compose.yml up -d
```

The compose file:

- Runs the `ghcr.io/iodsky/mysweldo-api:latest` image on port `8001`.
- Loads configuration from `.env` (point `CLOUD_DB_*` at the production PostgreSQL).
- Joins an external `traefik` network.
- Registers Traefik routing for `mysweldo-api.iodsky.com` over TLS (Let's Encrypt resolver `le`).

This requires a Traefik instance running on the host with the `traefik` network and `le` certresolver already configured, plus a DNS A record for `mysweldo-api.iodsky.com` pointing at the EC2 instance.

## CI/CD

`.github/workflows/ci-cd.yml`:

- Runs `./gradlew clean test` on every PR to `master`/`develop`.
- On `master`, builds the boot jar, builds and pushes the Docker image to GHCR.

## Project Structure

```
src/main/java/com/iodsky/mysweldo/
├── common/            # BaseModel, response envelopes, exception handling, config
├── <domain>/          # Controller, Service, Repository, Entity, Request, Dto, Mapper
├── payroll/           # core / strategy / run sub-packages
├── security/          # auth, jwt, role, user
├── imports/           # OpenCSV CSV imports (employees, users) + job tracking
└── Application.java   # entry point

src/main/resources/
├── application.yml         # shared config
├── application-local.yml   # local profile
├── application-prod.yml    # production profile
└── db/migration/           # Flyway migrations
```
