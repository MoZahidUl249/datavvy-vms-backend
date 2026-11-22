# Datavvy VMS backend — agent guide

Read this first. It applies to every agent (Antigravity, Claude Code, Codex,
Cursor …) and every human working in this repo.

**This repo:** the REST API — Spring Boot 3.5 · Java 21 · Maven multi-module
**modular monolith** · MySQL 8 (H2 in MySQL mode for tests) · Flyway.
**The web app** lives in [datavvy-vms-frontend](https://github.com/MoZahidUl249/datavvy-vms-frontend);
the two meet only at the API contract in `contracts/openapi/vms-api.yaml` (owned here).

## Repo map

```
AGENTS.md            ← you are here (always loaded)
.agents/rules/       ← scoped rules (load by file glob)
.agents/skills/      ← playbooks; each is also a /slash-command
.agents/agents/      ← read-only reviewer subagents
contracts/openapi/   ← API contract (change it FIRST)
vms-platform · vms-auth · vms-catalog · vms-vehicle · vms-dashboard · vms-app
API.md · postman/ · scripts/   ← API reference and test tools
docs/                ← architecture, ADRs, roadmap, team ownership
render.yaml · Dockerfile · docker-compose.yml   ← deployment
```

## Golden rules (non-negotiable)

1. **Module boundaries.** A module may use another module only through its
   `…<module>.api` package. `ArchitectureTest` fails the build on violations.
2. **Contract first.** Any change to a request/response/endpoint starts in
   `contracts/openapi/vms-api.yaml` (use `/api-change`), then Java, then the
   frontend repo's Dart models. Backend DTOs must match it field for field.
3. **Migrations are append-only.** Never edit a Flyway file that may have run.
   New file, timestamp version `VYYYY.MM.DD.HHMM__<module>_<what>.sql`, in the
   owning module's folder (use `/db-migration`). It must run on MySQL and H2.
4. **Errors are problem documents** with a stable `code`. Clients branch on
   `code`, never on message text.
5. **Tests travel with code.** Every behaviour change has a test in the same
   change. Don't weaken or delete a test to make it pass.
6. **No secrets in git.** Credentials come only from env vars (`VMS_DB_*`,
   `VMS_JWT_SECRET`) with no fallback values; `ConfigHygieneTest` enforces it.
7. **Stay in your lane.** Touch only the modules your task names. Changing
   `vms-platform` or the contract needs extra reviewers (CODEOWNERS).

## How to work (agent loop)

1. **Plan** — `/plan-feature` for anything bigger than a one-file fix; wait for
   approval on cross-module plans.
2. **Build** — `/new-backend-module`, `/api-change`, `/db-migration`.
3. **Verify** — `/verify`; paste the evidence.
4. **Review** — `architecture-reviewer` and `contract-guardian` subagents.
5. **Ship** — `/pr-ready`.

Parallel agents: one module per agent, each on its own branch/worktree.

## Definition of done

- [ ] `mvn verify` is green
- [ ] Tests added/updated for the behaviour change
- [ ] Contract and DTOs agree; frontend repo told about contract changes
- [ ] Schema change = new timestamped migration, works on MySQL and H2
- [ ] Every endpoint has `@PreAuthorize` (ADMIN / FLEET_MANAGER / VIEWER)
- [ ] Docs updated if behaviour or architecture changed (`API.md`, `docs/`)

## Modules

| Module | Package root | Owns | Public API (others may use) |
|---|---|---|---|
| `vms-platform` | `com.datavvy.vms.platform` | error model, paging, auditing base, roles | everything (shared kernel — keep tiny) |
| `vms-auth` | `…auth` | users, login, JWT, security filter chain | `auth.api` |
| `vms-catalog` | `…catalog` | vehicle types (`catalog_item`) | `catalog.api` (`CatalogLookup`, `CatalogUsageProvider` SPI) |
| `vms-vehicle` | `…vehicle` | fleet register (`vehicle`: vehicle number, company, type, brand, model, status) — API in `API.md` | `vehicle.api` (`VehicleQueryApi`) |
| `vms-dashboard` | `…dashboard` | nothing persisted; read model | `dashboard.api` |
| `vms-app` | `com.datavvy.vms` | boot class, config, seed data, cross-module tests | — |

Dependency direction: `platform ← auth`, `platform ← catalog ← vehicle ← dashboard`.
Never the other way. If catalog needs vehicle data, it defines an SPI in
`catalog.api` and vehicle implements it (see `CatalogUsageProvider`).

## Package layout inside a module

```
api/          public records + interfaces (the module's contract)
web/          @RestController only — no logic, map HTTP ⇄ application
application/  @Service use cases, transactions, validation that needs data
domain/       @Entity, repositories, value objects (no Spring web imports)
config/       @Configuration / @ConfigurationProperties
```

## Conventions

- Base path `/api/v1/<resource>`; guard every endpoint with `@PreAuthorize(Roles.…)`.
- Return `api` records, never entities. Lists return `PageResponse<T>`.
- Throw `NotFoundException`, `ConflictException`, `BusinessRuleException`,
  `FieldValidationException` — `GlobalExceptionHandler` renders RFC 9457 with `code`.
- Messages tell the user how to fix it, with an example ("Enter the model, like Hiace.").
- Aggregates extend `AuditableEntity` (created/updated at/by + `@Version`).
- Cross-module references are ids, no DB foreign keys across modules (ADR-0003).
- Migrations live in the owning module: `src/main/resources/db/migration/<module>/`,
  and the folder is listed in `vms-app/src/main/resources/application.yml`.
- Tests: unit tests in the module; HTTP/integration tests in `vms-app`
  (`VmsApiIntegrationTest`, H2 MySQL mode, seeded from `db/seed`).

## Commands

```bash
set -a; . ./.env; set +a        # credentials (run-backend.command creates .env)
docker compose up -d mysql      # optional; or add the h2 profile below
mvn verify                      # all tests incl. ArchitectureTest and ConfigHygieneTest
mvn install -DskipTests && mvn -f vms-app/pom.xml spring-boot:run -Dspring-boot.run.profiles=local      # MySQL
mvn install -DskipTests && mvn -f vms-app/pom.xml spring-boot:run -Dspring-boot.run.profiles=local,h2   # no MySQL
curl -s localhost:8080/actuator/health
bash scripts/api-smoke.sh       # live run-through: create, list, update, duplicate 409, delete
```

Demo users (`demo`/`local` profiles): `admin@datavvy.com / Admin@12345`,
`manager@datavvy.com / Manager@12345`, `viewer@datavvy.com / Viewer@12345`.
