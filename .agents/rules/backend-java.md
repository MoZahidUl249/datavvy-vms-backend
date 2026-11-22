---
trigger: glob
globs: "**/*.java, **/pom.xml, **/application*.yml"
description: "Spring Boot module, layering and API conventions for backend code."
---

# Backend rules

- Package layout per module: `api / web / application / domain / config`.
  Other modules may import **only** `…<module>.api`. `ArchitectureTest` enforces it.
- Controllers: thin, under `/api/v1/…`, return `api` records, guarded with
  `@PreAuthorize(Roles.CAN_READ | CAN_MANAGE_FLEET | CAN_ADMINISTER)`.
- Services: `@Transactional(readOnly = true)` at class level, `@Transactional` on commands.
- Expected failures → `NotFoundException`, `ConflictException`,
  `BusinessRuleException`, `FieldValidationException` with a stable UPPER_SNAKE `code`.
- User-facing messages say how to fix it with an example.
- Entities extend `AuditableEntity`; updates check `version` (optimistic locking).
- Constructor injection only; no field `@Autowired`.
- New module? Follow `/new-backend-module`; register it in `pom.xml`,
  `vms-app/pom.xml`, Flyway `locations`, `ArchitectureTest.MODULES`, CODEOWNERS.
- Tests: plain JUnit for domain logic; HTTP behaviour in `vms-app` integration tests.
