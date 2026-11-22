---
trigger: glob
globs: "**/db/migration/**/*.sql, **/db/seed/**/*.sql, **/domain/*.java"
description: "Flyway migration rules (MySQL 8 + H2 MySQL mode)."
---

# Database rules

- One folder per module: `<module>/src/main/resources/db/migration/<module>/`.
- File name: `VYYYY.MM.DD.HHMM__<module>_<verb>_<thing>.sql` (UTC time you wrote it).
  Timestamps avoid version clashes between teams; `out-of-order` is enabled.
- Applied migrations are immutable. Fix forward with a new file.
- Portable SQL only (must run on MySQL 8.4 **and** H2 `MODE=MySQL`): no
  `ENGINE=`, no `CHARSET` clauses, `DATETIME(6)` for instants, `BOOLEAN`, `BIGINT AUTO_INCREMENT`.
- Every table: `id`, audit columns (`created_at`, `updated_at`, `created_by`,
  `updated_by`, `version`).
- No foreign keys to another module's tables (ADR-0003); index columns you filter on.
- Demo data belongs in `vms-app/src/main/resources/db/seed/` (local profile only).
- Destructive changes use expand → migrate → contract across releases.
