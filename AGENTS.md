# Repository Guidelines

## Project Structure & Module Organization
- `apps/backend` holds the Spring Boot service (Java 17) with Liquibase changelogs under `src/main/resources/db` and application properties in `src/main/resources`.
- `apps/frontend` contains the Vite + React TypeScript client; shared UI primitives live in `src/components`, route-level views in `src/pages`, and global hooks in `src/hooks`.
- `infra/docker-compose.yml` orchestrates the PostGIS database, backend, and frontend; `scripts/seed.sql` and `.env.example` provide local database bootstrap values.

## Build, Test, and Development Commands
- `mvn -q -f apps/backend/pom.xml spring-boot:run` starts the API with live reload; use `mvn -f apps/backend/pom.xml clean package` for release builds.
- `npm install && npm run dev` inside `apps/frontend` launches the web client at `http://localhost:5173`; `npm run build` emits a production bundle in `dist`.
- `docker compose -f infra/docker-compose.yml up --build` spins up the full stack with PostGIS; export the variables from `.env.example` before first run.

## Coding Style & Naming Conventions
- Java code follows 4-space indentation, package names under `com.example.dating.backend`, and Lombok builders/records for DTOs; align controller routes with `/api/...` patterns.
- TypeScript uses strict mode, 2-space indentation, and PascalCase for React components; colocate Zustand stores in `src/store` and share hooks via camelCase file names.
- Keep configuration in `application.properties`; prefer constructor injection annotated with `@RequiredArgsConstructor` over field injection.

## Testing Guidelines
- Backend tests live in `apps/backend/src/test/java` and should end with `*Test`; use JUnit 5 with `@SpringBootTest` for integration scenarios and disable Liquibase when not needed.
- Run `mvn -f apps/backend/pom.xml test` before pushing; add focused unit tests for service logic even when covering controller flows.
- Frontend tests are not yet scaffolded—add Vitest or Playwright suites under `apps/frontend/src/__tests__` when introducing new behavior.

## Commit & Pull Request Guidelines
- Prefer Conventional Commits (`feat(scope): summary`, `fix:`, `chore:`); keep the subject in imperative voice and note module scopes such as `frontend` or `meeting`.
- Each PR should describe the user-facing effect, list manual/automated test results, and link relevant issues; include screenshots or GIFs for UI changes.
- Avoid bundling infrastructure updates with feature work unless tightly coupled; flag schema changes with Liquibase changelog references.

## Environment & Operations
- Copy `.env.example` to `.env` and supply `BOT_TOKEN`, `JWT_SECRET`, and Telegram URLs before using auth flows.
- Liquibase migrations run automatically on backend boot; verify new changelog files are referenced from `db/changelog/db.changelog-master.yaml`.
- Monitor backend health through `http://localhost:8080/actuator/health` and API docs at `/swagger-ui.html` during reviews to confirm service readiness.
