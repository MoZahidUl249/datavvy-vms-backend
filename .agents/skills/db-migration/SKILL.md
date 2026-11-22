---
name: db-migration
description: Writes a safe, portable Flyway migration (MySQL 8 and H2 MySQL mode) in the owning module's folder with timestamp versioning, audit columns and indexes, using expand/contract for destructive changes. Use for any schema or reference-data change, or when the user types /db-migration.
---

# Write a migration

1. **Where**: `<module>/src/main/resources/db/migration/<module>/`.
   Demo-only data → `vms-app/src/main/resources/db/seed/`.
2. **Name**: `V<YYYY>.<MM>.<DD>.<HHMM>__<module>_<verb>_<thing>.sql`, UTC now,
   e.g. `V2026.12.01.0930__vehicle_add_colour.sql`.
3. **Header**:
   ```sql
   -- Module: vehicle  | Owner: @datavvy/fleet-core-team
   -- Why: <one line>
   ```
4. **Portable SQL** (runs on MySQL 8.4 and H2 `MODE=MySQL`):
   - `BIGINT NOT NULL AUTO_INCREMENT`, `PRIMARY KEY (id)`
   - instants `DATETIME(6)`, flags `BOOLEAN`, text `VARCHAR(n)`
   - no `ENGINE=`, `CHARSET`, `COLLATE`, generated columns, or triggers
   - audit columns on new tables:
     ```sql
     created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
     created_by VARCHAR(120) NULL, updated_by VARCHAR(120) NULL,
     version BIGINT NOT NULL DEFAULT 0,
     ```
   - `CREATE INDEX idx_<table>_<col> ON <table> (<col>);` for filtered/sorted columns
   - no FK to another module's table
5. **Destructive change** (rename/drop/type change) = three releases:
   expand (add new, write both) → migrate data → contract (drop old).
6. **New module folder?** add it to `spring.flyway.locations` in
   `vms-app/src/main/resources/application.yml`, `application-local.yml`
   and `src/test/resources/application-test.yml`.
7. **Never** edit a migration that may have run anywhere. Fix forward.
8. **Verify**: `mvn verify` (runs Flyway on H2) and once
   against MySQL: `docker compose up -d mysql` + run the app with `local` profile.
