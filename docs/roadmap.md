# Roadmap

## Phase 1 — fleet register (this repo)
Sign-in · dashboard · vehicle CRUD with the course fields (Vehicle Number,
Company, Type, Brand, Model, Status) · list (status, search, sort, filters,
columns, paging) · segmented plate input · duplicate Vehicle Number rule ·
settings (types, columns, dashboard cards) ·
roles (admin / fleet manager / viewer).

## Phase 1.1 — hardening
- Persist session token (secure storage) and refresh tokens
- Persist UI preferences per user (`/api/v1/me/preferences`)
- Login rate limiting / lockout; password reset; user management screen
- Contract tests generated from `contracts/openapi` in CI
- Testcontainers MySQL integration tests alongside H2
- Turn on stricter Dart lints (`strict-casts`, `require_trailing_commas`)
- Facet dates via SQL for very large fleets

## Phase 2 — new modules (each = backend module + Flutter feature package)
| Module | Owns | Notes |
|---|---|---|
| maintenance | service records, schedules | reminders by date / odometer |
| fuel | fuel logs, cost per km | needs odometer readings |
| drivers | drivers, licences, assignment | links driver ⇄ vehicle |
| documents | registration, fitness, tax token, insurance expiry | file storage + expiry alerts |
| trips | trip log, utilisation | later: GPS integration |
| notifications | email/SMS digests | consumes events from other modules |

Use `/plan-feature`, then `/new-backend-module` + `/new-flutter-feature`.

## Phase 3 — scale
Domain events between modules (Spring Modulith events or outbox) → extract
hot modules into services if needed; Spring Boot 4 upgrade (ADR-0006).
