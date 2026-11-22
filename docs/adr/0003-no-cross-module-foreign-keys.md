# ADR-0003: No foreign keys across module tables

**Status:** accepted · 2026-11-21

## Decision
A module owns its tables. References to another module's data are plain values
(`vehicle.type` holds a catalog type name), validated in the application layer
through the other module's API (`CatalogLookup.findByName`). Foreign keys are fine **within** a module.

## Consequences
+ Modules can be migrated, split or extracted independently.
+ No hidden coupling through cascades.
− Integrity is application-enforced: the catalog refuses to delete or rename
  types in use (via the usage SPI).
