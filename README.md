# serviceschedule

An application to manage service schedules and participants – plus bug/feature requests, an alarm system event log, user rights and aircraft maintenance (kanban tasks, templates, defects).

| Part | Stack | Directory |
| --- | --- | --- |
| Backend | Spring Boot 3.5, Java 25, PostgreSQL, Flyway | [schedule-api/](schedule-api/) |
| Frontend | Nuxt 4 (SPA), Nuxt UI 4, Pinia, i18n (de/en) | [schedule-ui/](schedule-ui/) |

Notes on code structure, conventions, roles/rights and the maintenance module are in [CLAUDE.md](CLAUDE.md).

## Prerequisites

- Java 25 (JDK), Maven 3.8+ (or the bundled wrapper `./mvnw`)
- Node.js 22+ and pnpm
- Docker or Podman (for the database or the whole stack)

## Local development

**1. Start the database** (PostgreSQL on port 5432, user/database `schedule`, password `password`):

```bash
docker compose -f docker-compose-local-db.yml up -d
```

**2. Start the backend** – Flyway creates or migrates the schema on startup:

```bash
cd schedule-api
./mvnw spring-boot:run
```

The API runs at http://localhost:8080/api/v1, Swagger UI at http://localhost:8080/api/v1/swagger-ui/index.html, the health check at http://localhost:8080/api/v1/actuator/health.

**3. Start the frontend:**

```bash
cd schedule-ui
pnpm install
pnpm dev
```

The UI runs at http://localhost:3000 and talks to the API configured as `API_URL` in `schedule-ui/.env` (default: `http://localhost:8080/api/v1/`).

**First user:** Only people set up as participants with a registration key can sign up. Roles are assigned in the database, e.g. for an administrator:

```sql
INSERT INTO auth_user_roles (user_id, role_id)
SELECT u.id, r.id FROM auth_user u, auth_role r
WHERE u.username = 'admin@example.org' AND r.name = 'ROLE_ADMIN';
```

Administrators then grant further rights (manage schedules, alarm system, maintenance) in the app under **Users**.

## Tests

```bash
cd schedule-api && ./mvnw test      # backend: unit and integration tests against H2, no database needed
cd schedule-ui && pnpm test         # frontend: Vitest, API mocked
cd schedule-ui && pnpm test:e2e     # E2E: Playwright against a production build, API mocked
                                    # (once: pnpm exec playwright install chromium webkit)
```

## Build

```bash
cd schedule-api && ./mvnw clean package            # target/schedule-api.jar (-DskipTests to skip tests)
cd schedule-ui && pnpm exec nuxt generate          # static files in .output/public
cd schedule-ui && pnpm exec nuxt generate --dotenv .env.production   # against the production API
```

## Run the whole stack (containers)

`build-and-run.sh` builds API and UI and starts database, API and nginx with Podman Compose:

```bash
./build-and-run.sh              # build and start (UI: http://localhost:8081, API: http://localhost:8080/api/v1)
./build-and-run.sh --tests      # run the backend tests during the build
./build-and-run.sh --prod       # build the UI against the API URL from schedule-ui/.env.production
./build-and-run.sh --no-build   # start the last build
./build-and-run.sh stop | logs | status
```

This needs two files that are not in the repository:

- `.env` in the root directory, at least `CORS_ALLOWEDORIGINS=http://localhost:8081`
- `schedule-api/src/main/resources/application-docker.properties` (profile `docker`: database `db:5432`, JWT secret, CORS, logging; template: `application.properties`)

With Docker instead of Podman: build backend and UI as above, then run `docker compose up -d --build`.

The database files live in `volumes/postgres`. New Flyway migrations (e.g. `V3.2__maintenance.sql` for the maintenance module) run automatically the next time the API starts.
