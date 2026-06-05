# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start local PostgreSQL
docker compose -f db.compose.yml up -d

# Run the app locally (sources .env, then runs Spring Boot)
./run.sh

# Build
./mvnw clean package

# Run all tests
./mvnw clean test

# Run a single test class
./mvnw test -Dtest=BenefitServiceTest

# Run a specific test method
./mvnw test -Dtest=BenefitServiceTest#methodName
```

Copy `.env.template` to `.env` and fill in database credentials before running locally. The app uses Spring profile `local` by default via `run.sh`.

## Architecture

**Stack**: Spring Boot 3.5.6 · Java 21 · PostgreSQL 16 · Flyway · JWT (JJWT) · Spring Security · Spring Batch · SpringDoc OpenAPI

**Base URL**: `/api` — Swagger UI at `/api/swagger-ui.html`

### Module layout

Every domain module under `src/main/java/com/iodsky/mysweldo/` follows the same layered pattern:

```
<domain>/
  <Domain>Controller.java      # REST endpoints
  <Domain>Service.java         # Business logic
  <Domain>Repository.java      # Spring Data JPA
  <Domain>Entity.java          # JPA entity (extends BaseModel)
  <Domain>Request.java         # Inbound DTO
  <Domain>Response.java        # Outbound DTO
  <Domain>Mapper.java          # Entity ↔ DTO conversion
```

Domains: `attendance`, `batch`, `benefit`, `contribution`, `deduction`, `department`, `employee`, `leave`, `overtime`, `pagIbig`, `payroll`, `philhealth`, `position`, `security`, `sss`, `tax`

### Shared infrastructure (`common/`)

- `BaseModel` — superclass for all entities; provides `id`, `createdAt`, `updatedAt`, `createdBy`, `lastModifiedBy`, `version` (optimistic locking), and `deletedAt` (soft delete).
- `@SQLRestriction("deleted_at IS NULL")` on entities enables automatic soft-delete filtering.
- Global exception handler returns consistent `ApiResponse` envelopes.

### Payroll engine (`payroll/`)

The payroll calculation lives in `payroll/core/PayrollCalculator` (@Component). It:
- Loads SSS, Pag-IBIG, PhilHealth, and tax bracket rates for the payroll period date.
- Applies overtime (1.25×), taxable/non-taxable benefit splits, and semi-monthly/daily rate logic.
- Is invoked by `PayrollRunService` which orchestrates a full `PayrollRun` across all active employees.
- Uses a builder (`PayrollItemBuilder`) and strategy pattern (`payroll/strategy/`) to compose line items.

### Security

JWT is issued at `/auth/login` and `/auth/refresh`. All `/auth/**`, `/docs/**`, and `/swagger-ui/**` paths are public; everything else requires a valid Bearer token. Roles are stored in the `role` table and enforced via Spring Security method security.

### Database migrations

Flyway migrations live in `src/main/resources/db/migration/`. Add new migrations as `V{n}__description.sql`. Never modify existing migration files — Flyway runs in `validate` mode in both profiles.

### Spring profiles

| Profile | Activation | Database |
|---------|-----------|----------|
| `local` | default in `run.sh` | `localhost:5432` via `.env` |
| `prod`  | set by CI/CD deploy | Cloud PostgreSQL via env vars |

### CI/CD

GitHub Actions (`.github/workflows/ci-cd.yml`): tests run on all PRs; Docker image is built, pushed to GHCR, and SSH-deployed to VPS only on `master` merges. Traefik handles HTTPS at `mysweldo-api.iodsky.com`.
