---
name: new-backend-module
description: Scaffolds a new Spring Boot bounded-context module (e.g. maintenance, fuel-log, driver) in the modular monolith with api/web/application/domain packages, Flyway folder, security, tests and wiring. Use when adding a new backend capability that owns its own data, or when the user types /new-backend-module.
---

# Add a backend module

Example used below: **maintenance** (service records for vehicles).
Replace `maintenance` / `Maintenance` with your module name.

## 1. Create the Maven module

`vms-maintenance/pom.xml` — copy `vms-catalog/pom.xml`, change
`artifactId`/`name`, and depend only on what you need:

```xml
<dependency><groupId>com.datavvy.vms</groupId><artifactId>vms-platform</artifactId></dependency>
<dependency><groupId>com.datavvy.vms</groupId><artifactId>vms-vehicle</artifactId></dependency> <!-- only for vehicle.api -->
```

Register it in three places:
- `pom.xml` → `<modules>` and `<dependencyManagement>`
- `vms-app/pom.xml` → `<dependencies>`
- `vms-app/src/main/resources/application.yml` (+ `application-local.yml`,
  test `application-test.yml`) → `spring.flyway.locations: classpath:db/migration/maintenance`

## 2. Packages

```
com.datavvy.vms.maintenance
├── package-info.java          // purpose + owning team
├── api/                       // MaintenanceRecordRequest/Response records, MaintenanceQueryApi (if others read it)
├── web/MaintenanceController  // /api/v1/maintenance-records, @PreAuthorize on every method
├── application/MaintenanceService
└── domain/MaintenanceRecord (extends AuditableEntity), MaintenanceRecordRepository
```

Rules: entities never leave the module; reference vehicles by `vehicleId`
(validate through a `vehicle.api` interface — add one there if missing, with
Fleet Core review). No FK to `vehicle` (ADR-0003).

## 3. Migration

`vms-maintenance/src/main/resources/db/migration/maintenance/V<YYYY.MM.DD.HHMM>__maintenance_create_maintenance_record.sql`
— follow `/db-migration` (audit columns, portable SQL, indexes on `vehicle_id`
and dates).

## 4. Errors & security

- Codes: `MAINTENANCE_RECORD_NOT_FOUND` (via `NotFoundException("Maintenance record", id)`),
  domain conflicts via `ConflictException("MAINTENANCE_…", message)`.
- Reads `Roles.CAN_READ`; writes `Roles.CAN_MANAGE_FLEET` (or a new role —
  add it to `platform.security.Roles` with a platform-team review).

## 5. Contract

Add paths + schemas to `contracts/openapi/vms-api.yaml` first (`/api-change`).

## 6. Tests

- Domain rules: JUnit in the module (`src/test/java/...`).
- HTTP: add cases to a new `MaintenanceApiIntegrationTest` in `vms-app`
  (copy the structure of `VmsApiIntegrationTest`).
- Add `"maintenance"` to `ArchitectureTest.MODULES`.

## 7. Ownership & docs

- `.github/CODEOWNERS`: `/vms-maintenance/ @datavvy/<team>`
- `docs/team-ownership.md` and the module table in `AGENTS.md`.

## 8. Verify

```bash
mvn verify
```
Then call the new endpoints through Swagger UI with a token from `/api/v1/auth/login`.
