# AGENTS.md

Spring Boot 3.5.6 · Java 21 · Gradle · PostgreSQL 16 · Flyway · Spring Security (JWT) · Spring Batch · SpringDoc.

## Commands

```bash
# Start local PostgreSQL (+ pgAdmin at :5050); needs .env first
docker compose -f db.compose.yml up -d

# Run app locally (sources .env, then ./gradlew bootRun, profile=local)
./run.sh

# All tests
./gradlew clean test

# Single test class / method
./gradlew test --tests "com.iodsky.mysweldo.benefit.BenefitServiceTest"
./gradlew test --tests "com.iodsky.mysweldo.benefit.BenefitServiceTest.methodName"
```

- Copy `.env.template` → `.env` and fill DB/JWT values before running. `.env` is loaded via `spring.config.import: optional:file:.env[.properties]` — not through Gradle, so new env vars must be added to `.env.template`.
- All endpoints are under `/api` (server context-path). Swagger UI at `/api/swagger-ui.html`, OpenAPI JSON at `/api/docs`.
- CI (`.github/workflows/ci-cd.yml`) runs `./gradlew clean test` on all PRs to `master`/`develop`; on `master` it builds `./gradlew clean bootJar`, builds + pushes the Docker image to GHCR. The SSH deploy step is commented out.

## Architecture

Every domain module under `src/main/java/com/iodsky/mysweldo/<domain>/` follows the same layered pattern:

```
<Domain>Controller.java   # REST endpoints
<Domain>Service.java      # business logic (@Service, @RequiredArgsConstructor)
<Domain>Repository.java   # Spring Data JPA
<Domain>Entity.java       # JPA entity extends BaseModel
<Domain>Request.java      # inbound DTO
<Domain>Dto.java          # outbound DTO
<Domain>Mapper.java       # Entity <-> DTO conversion (also used for Request -> Entity)
```

Domains: `attendance`, `batch`, `benefit`, `contribution`, `deduction`, `department`, `employee`, `leave`, `overtime`, `pagIbig`, `payroll`, `philhealth`, `position`, `security` (auth/jwt/role/user), `sss`, `tax`.

### Shared infrastructure (`common/`)

- `BaseModel` is the superclass of every entity: provides `createdAt`/`updatedAt`, `createdBy`/`lastModifiedBy` (JPA auditing), `version` (optimistic locking), and `deletedAt` (soft delete). Entities are annotated `@SQLRestriction("deleted_at IS NULL")` so soft-deleted rows are auto-filtered — don't add manual `deleted_at` filters on top.
- All responses are wrapped through `common/response/ResponseFactory` (`ApiResponse` envelope, optional `PaginationMeta`). Errors flow through `GlobalExceptionHandler`.
- Errors are raised as `ResponseStatusException` in services.

### Payroll engine (`payroll/`)

Non-trivial; split into three sub-packages with distinct responsibilities:
- `payroll/core` — `PayrollCalculator` (@Component) + `PayrollItemAssembler` compose statutory deductions/contributions and line items.
- `payroll/strategy` — pay-basis strategies (`PayBasisStrategyFactory` selects hourly/daily/monthly) and `StandardPayrollComputationStrategy`. Add a new pay type here.
- `payroll/run` — `PayrollRunService` orchestrates full payroll runs across active employees.

When touching payroll, check the existing strategy classes first — logic is heavily delegated, not centralized.

### Security

JWT issued at `/auth/login` and `/auth/refresh`. Only `/auth/**`, `/docs/**`, `/swagger-ui/**` are public; everything else requires a valid Bearer token. Role-based access is enforced via Spring method security (`@EnableMethodSecurity`).

### Batch / uploads

Spring Batch jobs are disabled on startup (`spring.batch.job.enabled: false`); they're launched on demand via `batch/BatchController`. File uploads are capped at 10MB and stored under `uploads/` (configured via `batch.upload.directory`).

## Database

- Flyway migrations in `src/main/resources/db/migration/` as `V{n}__description.sql`. **Never modify an existing migration** — both profiles run `ddl-auto: validate`, and Flyway checksums will fail. Add a new `V{n+1}__...sql` instead.
- Currently only `V1__initial_schema.sql` and `V2__...` exist; `src/main/resources/db/migration/` is the only schema source of truth.

## Spring profiles

| Profile | Activation | DB |
|---------|-----------|----|
| `local` | default via `./run.sh` | `localhost:5432` from `.env` (LOCAL_DB_*) |
| `prod`  | set at CI/CD deploy | Cloud PostgreSQL from `.env` (CLOUD_DB_*) |

## Conventions

- Controllers return `ResponseFactory` envelopes; services throw `ResponseStatusException`. Match the existing pattern in the domain you touch rather than inventing a new response shape.
- Tests use JUnit 5 + AssertJ, are named `<Domain>ServiceTest`, and live under `src/test/java/com/iodsky/mysweldo/<domain>/`. Stubs for repository/view interfaces are colocated (e.g. `EmployeeBasicStub`, `AttendanceViewStub`).
