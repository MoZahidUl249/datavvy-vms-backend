# ADR-0007: Fleet records as three modules, linked by vehicle id and a delete event

**Status:** accepted · 2026-11-24

## Context
The fleet-management extension adds vehicle documents (with expiry), maintenance
records and drivers (with vehicle assignment). They have different owners,
release cadences and rules, and all of them hang off a vehicle.

## Decision
1. **Three modules** — `vms-document` (Fleet Compliance), `vms-maintenance` and
   `vms-driver` (Fleet Operations). Each owns its tables and Flyway folder and
   depends on `vehicle.api` only. The dashboard reads them through their
   `*QueryApi` interfaces. Dependency direction:
   `platform ← catalog ← vehicle ← {document, maintenance, driver} ← dashboard`.
2. **Vehicle by id** (ADR-0003). Existence is checked through
   `VehicleQueryApi.find`; lists join vehicle numbers with `findAll(ids)`;
   "search by vehicle number" uses `idsWithNumberLike(q)`.
3. **Deleting a vehicle** publishes `VehicleDeletedEvent` (in `vehicle.api`)
   inside the delete transaction. Documents and maintenance delete their rows;
   the driver module closes the open assignment and keeps history (rows carry
   name snapshots). A failing listener rolls the whole delete back. The vehicle
   module never knows who listens.
4. **One open assignment per vehicle and per driver**, enforced twice: the
   service checks, and `driver_assignment.open_vehicle_id` / `open_driver_id`
   are UNIQUE columns that hold the ids only while the assignment is open
   (NULL after release; NULLs never clash on MySQL or H2). Same pattern as
   `vehicle_number_key`.
5. **Document files live in the database** (`vehicle_document_file`, separate
   from the metadata so lists never read bytes) behind `DocumentContentStore`.
   Free hosting has no persistent disk, and keeping files next to their
   metadata means one backup and no orphaned objects. The file type is detected
   from its first bytes (PDF / JPG / PNG / WEBP), not from the browser's claim;
   max 5 MB (`vms.documents.max-file-size`).
6. **One expiry rule** (`platform.time.Expiry`) for documents and licences:
   EXPIRED before today, EXPIRING_SOON within `expiring-soon-days` (30),
   VALID after, NO_EXPIRY without a date; worked out in Asia/Dhaka.

## Consequences
+ Teams work on documents, maintenance and drivers in parallel; ArchitectureTest
  keeps them out of each other's internals.
+ Any of the three can become a service later: it already talks to vehicles
  only through an interface and an event.
− Events are in-process and synchronous. If a module is extracted, switch to an
  outbox (roadmap Phase 3).
− Files in MySQL grow the database. At scale, implement `DocumentContentStore`
  on S3 / Cloudflare R2 and keep only the key in the row; the service and API
  don't change.
