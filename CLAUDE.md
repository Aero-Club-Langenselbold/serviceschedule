# CLAUDE.md

Notes for working on this repo.

## What this is

Member area of a gliding club (Aero-Club Langenselbold): service schedules (participants, services, plans, availabilities, PDF/CSV export), bug & feature requests with comments, an alarm system event log, user/permission administration and the **maintenance module** (aircraft, kanban tasks, task templates, defects/findings – the functionality of [vereins-wartung](https://github.com/Double-N-Studio/vereins-wartung), rebuilt on this stack).

- `schedule-api/` – Spring Boot 3.5 backend (Java 25, Maven, PostgreSQL, Flyway, JPA/Hibernate, Spring Security with JWT), REST under context path `/api/v1`
- `schedule-ui/` – Nuxt 4 SPA (`ssr: false`) with Nuxt UI 4, Pinia, `@nuxtjs/i18n` (de/en), Tailwind 4; deployed as static files (`nuxt generate`) behind nginx
- Root: `docker-compose.yml` (db + api + nginx), `build-and-run.sh` (Podman), `docker-compose-local-db.yml` (only a local Postgres)

## Commands

```bash
# local database only (user/db "schedule", password "password", port 5432)
docker compose -f docker-compose-local-db.yml up -d

cd schedule-api
./mvnw spring-boot:run          # http://localhost:8080/api/v1, Swagger UI under /api/v1/swagger-ui/index.html
./mvnw test                     # H2 in memory, ~220 tests (unit + @SpringBootTest integration)
./mvnw -Dtest=MaintenanceIntegrationTest test

cd schedule-ui
pnpm install
pnpm dev                        # http://localhost:3000, API from API_URL in .env
pnpm test                       # vitest (unit/component tests, API mocked in tests/setup.ts)
pnpm test:e2e                   # playwright against a production build, API mocked in e2e/support/mock-api.ts
pnpm exec nuxt generate         # static build to .output/public

./build-and-run.sh              # whole stack in Podman (see --help); UI http://localhost:8081, API http://localhost:8080
```

Run the backend tests when `schedule-api` changed and `pnpm test` when `schedule-ui` changed. The first vitest run after a cold start is occasionally flaky (workers time out, `localStorage` undefined) – rerun before investigating.

## Backend conventions

- Package per module under `de.ct.manager.schedule` (`participant`, `service`, `plan`, `request`, `alarm`, `user`, `maintenance` …): entity, repository, service, controller, DTOs.
- Entities extend `util.AbstractPersistentEntity` (`id` = UUID string, `@Version version` for optimistic locking). `OptimisticLockingFailureException` becomes `409` in `GlobalExceptionHandler`.
- **Schema changes only through Flyway** (`src/main/resources/db/migration/V<major>.<minor>__name.sql`, never edit an applied migration). Write SQL that runs on PostgreSQL *and* H2: the tests run Flyway on H2 first and then let Hibernate recreate the schema from the entities (`ddl-auto=create-drop`). So entity mappings must match the SQL (column names, lengths, `@OnDelete` for `ON DELETE CASCADE/SET NULL`), otherwise production and tests diverge.
- Clean structures: foreign keys with explicit `ON DELETE` behaviour, check constraints for enums stored as strings, unique constraints, indexes on foreign keys used for lookups.
- Errors: throw `ResponseStatusException` (→ `{ error }` with the status) or `AccessDeniedException` (→ 403). Validation via `jakarta.validation` on request records (→ 400).
- Integration tests extend `util.AbstractIntegrationTest` (real HTTP via `TestRestTemplate`, JWT for `test@test.com` = `ROLE_USER` or `admin@test.de` = `ROLE_ADMIN`, users from `scripts/test-users.sql`). Clean up your tables with `@Sql(..., AFTER_TEST_METHOD)`; with method level `@Sql` add `@SqlMergeMode(MERGE)` so the class level cleanup is not replaced. Delete rows referencing `auth_user` before the class teardown deletes the users.

## Roles and rights

- Roles (`UserRole`, table `auth_role`): `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SCHEDULE_ADMIN`, `ROLE_ALARM_ADMIN`, `ROLE_MAINTENANCE_ADMIN`. Administrators assign them on the users page (`GET /user/roles` = `UserRole.assignable()`, `PUT /user/{id}/roles`): everything except `ROLE_USER`, which stays as it is (a user left without roles gets it); administrators cannot revoke their own `ROLE_ADMIN` (`400`). Missing `auth_role` rows are created on demand (the test data seeds only three). Authorities are read from the database on every request, so changes apply at once.
- Rights (`UserPermission`, table `auth_user_permissions`) are granted by administrators at runtime on the users page: `PERMISSION_SCHEDULE_MANAGE`, `PERMISSION_ALARM_MANAGE`, `PERMISSION_MAINTENANCE_MANAGE`.
- `WebSecurityConfig#roleHierarchy`: `ROLE_ADMIN > PERMISSION_* > ROLE_<MODULE>_ADMIN`. So `@PreAuthorize("hasRole('MAINTENANCE_ADMIN')")` admits administrators, maintenance admins and holders of the right. Checks inside services use the hierarchy too (`MaintenanceAccess#canManage`), never compare role names directly.
- Frontend mirrors this in `stores/user.ts` (`canManageSchedule`, `canManageAlarm`, `canManageMaintenance`) and in route middlewares (`middleware/*-admin.ts`). A new right also needs: i18n names under `users.permissionNames` / `users.roleNames`, and `permissionsOfRole` in `pages/users/list.vue`.
- The frontend only hides controls; the backend enforces every rule.

## Plan generation and requests

- After `POST /plan/generate/{id}` the plan list fetches `GET /plan/unfilled/{id}` (services of the plan with fewer people than `mandatoryAssignees`, by day) and shows them in `components/plan-unfilled-dialog.vue`.
- `POST /request/{id}/comments` takes an optional `status` (`CLOSED`/`REJECTED`): administrators close or reject an entry with the comment (split button in `components/request-comments.vue`), anybody else gets `403`.

## My services

`GET /plan/mine` (`?past=true` includes past services) and `/plan/mine/ics` list the plan entries of the signed in user: user ↔ participant by e-mail, only published plans, with the colleagues of the same service and time. Page `pages/plan/mine.vue`. Plan times are local wall clock times (`LocalDateTime`); the ICS export converts them to UTC with `schedule.calendar.timezone` (default `Europe/Berlin`).

## Maintenance module

Backend `schedule-api/.../maintenance/`, migrations `V3.2__maintenance.sql`, `V3.4__maintenance_workshop_schedule_inventory.sql`, `V3.5__maintenance_task_workshop_slot.sql` and `V3.6__defect_book_and_notifications.sql`, frontend `pages/maintenance/`, `components/maintenance/`, `composables/useMaintenance.ts`, `types/maintenance.ts`. API under `/api/v1/maintenance`.

Tables: `maintenance_aircraft` → `maintenance_task` (+ `_checklist_item`, `_note`, `_history`), `maintenance_defect` (+ `_assignee`, `_event`) and `maintenance_workshop_slot`; `maintenance_template` (+ `_aircraft_type`, `_checklist_item`); `maintenance_inventory_item` (+ `_aircraft_type`) → `maintenance_inventory_history`. Deleting an aircraft cascades to its tasks, defects and workshop periods; deleting a template, defect or workshop period only unlinks tasks; deleting a user keeps the records (`SET NULL`).

Rules (enforced in `MaintenanceTaskService` / `MaintenanceDefectService`):

- Every signed in user (menu entry "Wartung"): sees all aircraft, works on tasks (start, back to open, report done, reopen), ticks checklist steps, adds notes, reports and works on defects (see defect book).
- Maintenance administrators: aircraft and templates (CRUD), create/edit/delete tasks, "Aus Vorlagen anlegen" (dialog `components/maintenance/template-import.vue`: the admin picks templates, nothing preselected; `POST /aircraft/{id}/import-templates` with `{ workshopId, templateIds }` creates a task per chosen template on that board, skips templates already on it, `400` for templates not applying to the aircraft type; templates with no aircraft types apply to all), sign off and withdraw sign-off, delete any note, assign/sign off/edit/delete any defect.
- Status: `OPEN → IN_PROGRESS → DONE`. The checklist drives it: the first tick starts the task, the last tick finishes it, unticking a done task reopens it. The other way round, finishing a task (button or drag to `DONE`) and signing it off tick all remaining steps (`completeChecklist`); reopening leaves them ticked. Signed off tasks are locked (`409`) for everything except withdrawing the sign-off and deleting.
- Cards are moved between the board columns by drag and drop (native HTML5 DnD, `PUT /maintenance/tasks/{id}/status`). Jumps over a column are logged as the single steps (OPEN → DONE = `STARTED` + `FINISHED`).
- History is append-only and stores an action enum (`TaskHistoryAction`); the UI translates it (`maintenance.task.actions.*`).
- Editing a task's checklist keeps ticked steps whose text is unchanged; items are updated in place by position (unique `(task_id, position)`).
- Notes: authors delete their own, maintenance administrators all. Responses carry `deletable`/`editable` so the UI does not duplicate those rules.
- A task resolves at most one defect of the same aircraft, a defect is linked to at most one task.
- People are returned as `{ id, name }`, name = participant full name if one exists for the username, else the username.

Workshop schedule (`/maintenance/workshop`, page `pages/maintenance/schedule.vue`, Gantt chart):

- Every period is a **workshop visit with its own board**: `maintenance_task.workshop_slot_id` (nullable, same aircraft, `400` otherwise). `GET /aircraft/{id}/tasks?workshopId=` returns the board of a visit, without the parameter the board "Ohne Werkstattzeitraum" (tasks outside the workshop, e.g. defect fixes). The board page picks the visit via `?visit=<slotId|none>`, default: the visit running today or else the next one, else the board without period – finished visits open only when chosen (own group "Abgeschlossene Aufenthalte" in the select). The overview cards (`pages/maintenance/index.vue`) link every running/upcoming visit with its progress; finished ones appear after "… abgeschlossene Aufenthalte anzeigen". A visit with tasks cannot move to another aircraft (`409`).
- Maintenance administrators plan periods (`maintenance_workshop_slot`, dates inclusive) – an aircraft any number of times, periods of different aircraft may overlap, periods of the same aircraft must not (`409`). Everybody sees the chart.
- A click on a bar opens a dropdown below it (`components/maintenance/workshop-slot-panel.vue` in a `UPopover`): period, progress and a link to the board of that visit (`/maintenance/{aircraftId}?visit={slotId}`); maintenance administrators change dates/note or delete there. The modal `workshop-slot-form.vue` is only used for new periods and the edit button in the list.
- `WorkshopSlotResponse` carries the progress of its board: `taskCount`/`doneCount`/`signedCount` and `stepCount`/`stepDoneCount` (checklist steps; a task without checklist counts as one step, done when the task is `DONE`). Every bar shows its own progress; the row header and the overview card show `AircraftResponse.workshop` (the period running today or else the next one). `AircraftResponse.unscheduledTaskCount` counts the tasks without period.

Defect book (`/maintenance/defects`, page `pages/maintenance/defects.vue`, components `defect-detail.vue`/`defect-form.vue`, menu entry "Mängelbuch"):

- A defect belongs to an aircraft or other equipment (a winch is just a `maintenance_aircraft` with type "Winde"), never to a workshop visit. The board page only links to the book (`?aircraft=<id>`); "Aufgabe erstellen" in the detail opens the task form on the board via `?newTaskFor=<defectId>`.
- `GET /maintenance/defects` (optional `?aircraftId=`) lists all; the page filters client side by aircraft, status (Offen = `OPEN`/`IN_PROGRESS`, Abnahme offen = `DONE`/`REJECTED` not signed, Abgenommen, Alle) and "nur mir zugewiesene". Filters and the open defect live in the URL (`?aircraft=&status=&mine=1&defect=`).
- Everybody works on every defect: `PUT /defects/{id}/status` with `OPEN`/`IN_PROGRESS`/`DONE`. Only maintenance admins reject (`REJECTED`, needs a `comment`, else `400`) and take a rejection back (`403` for others). Everybody comments (`POST/DELETE /defects/{id}/comments`, authors and maintenance admins delete). Reporters, assignees and maintenance admins edit title/description/priority; only maintenance admins change assignees (`assigneeIds` in the request, `null` = unchanged) and list `GET /defects/assignable-users`. Reporters delete until sign-off, maintenance admins always.
- Only maintenance admins sign off (`POST /defects/{id}/sign`, only `DONE`/`REJECTED`, else `409`) and withdraw (`DELETE`). Signed off defects are locked (`409`) for everything except withdrawing and deleting.
- Activity log `maintenance_defect_event` (append-only, action enum `DefectEventAction`, translated as `maintenance.defect.actions.*`; only `COMMENTED` entries can be deleted). Write endpoints answer with `DefectDetail` `{ defect, events }`.
- `openDefectCount` counts `OPEN` + `IN_PROGRESS`.

Notifications (package `notification`, table `user_notification`, `components/notification-bell.vue` next to the account menu): assigning a defect notifies the assignee (not oneself); taking the assignment back removes the unread notification; deleting the defect removes them all. `GET /notifications` (latest 30 + `unreadCount`), `PUT /notifications/{id}/read` (own only, else `404`), `PUT /notifications/read-all`. The bell refreshes on open, on route change and every minute; a click marks read and opens `/maintenance/defects?defect=<id>`.

Inventory (`/maintenance/inventory`, page `pages/maintenance/inventory.vue`), modelled on the "Inventur" of vereins-wartung:

- Items are consumables or spare parts (spare parts list the aircraft types they fit, none = all), counted as `COUNT` (units, optional unit and minimum stock) or `LEVEL` (0 = empty, 1 = low, 2 = enough). The stock status `OK`/`LOW`/`EMPTY` is derived (`MaintenanceInventoryItem#getStatus`).
- Every signed in user counts (`PUT /maintenance/inventory/{id}/quantity`); maintenance administrators create, edit (incl. comment) and delete items. History is kept per item; counts of the same user within 15 minutes are merged into one entry, so a stepper does not flood it.

Not ported from vereins-wartung (possible follow-ups): seasons/archive, leads per workshop day, documents and file/photo uploads, deadlines and the Vereinsflieger sync, the order list of the inventory, FAQ, offline sync.

## Frontend conventions

- Pages use `AppPage` (title, description, `#actions`, error/success messages, width). Tables use `DataTable` (turns into cards below `md`). Delete confirmation via `ConfirmationDialog`. Lists of cards: `rounded-xl border border-default bg-default p-4`, like the home page tiles.
- Light theme only; colors via Nuxt UI semantic colors (`primary` teal, `neutral` stone …, see `app.config.ts`) and tokens (`text-muted`, `bg-elevated` …), no hex values. Touch-friendly sizes below `lg` come from `app.config.ts`.
- API calls through `useAPI` (returns `{ data, error }`, never throws; `error.value.statusCode` for status specific handling). The plugin `plugins/api.ts` adds the JWT and redirects to `/login` on 401.
- All user facing text through i18n in **both** `i18n/locales/de-DE.ts` and `en-US.ts`. Tests run in English.
- `USelect` items need non-empty values: use sentinels (`NO_SELECTION`, duration `0`) and map to `null` in the form.
- Component tests: `mountSuspended`; API answers are mocked in `tests/setup.ts` (add new endpoints and test users there). Components that render `UTooltip` (e.g. `ConfirmationDialog`) need `global.stubs` in page tests, the test mount has no `UApp` tooltip provider.

## Member guide

`Anleitung-Dienstplan.pdf` (root) is a step-by-step guide for members (log in via Vereinsflieger with security question and 2FA code, enter availabilities, published plan, my services, mobile). It assumes the production setup: `vfLoginFeature` and `securityQuestionFeature` active, so there is no registration; the security answer is deliberately not shown. Sources in `docs/anleitung-dienstplan/`: `anleitung.html` (text) and `generate.cjs` (screenshots with invented sample data against the static UI build, then Chromium prints the PDF). Regenerate after UI changes to those pages: `(cd schedule-ui && pnpm exec nuxt generate) && NODE_PATH=schedule-ui/node_modules node docs/anleitung-dienstplan/generate.cjs`.

## Configuration

- `schedule-api/src/main/resources/application.properties` – local defaults (Postgres on localhost, CORS for `localhost:3000`/`8081`).
- `application-docker.properties` – production profile, **not in git**, required by `build-and-run.sh`.
- Root `.env` – `CORS_ALLOWEDORIGINS` etc. for compose, **not in git**.
- `schedule-ui/.env` / `.env.production` – `API_URL` the UI talks to.
