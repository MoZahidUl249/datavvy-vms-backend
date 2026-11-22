# ADR-0002: Modules talk only through `api` packages

**Status:** accepted · 2026-11-21

## Decision
Each backend module exposes a `…<module>.api` package (records + interfaces).
Other modules may import nothing else from it. When a provider module needs
information from a consumer (e.g. catalog needs usage counts from vehicle), the
provider declares an SPI interface in its `api` and the consumer implements it
(`CatalogUsageProvider`).

Flutter mirrors this: feature packages never import each other; shared code
lives in `vms_core`, `vms_design_system` or data packages.

## Enforcement
`vms-app/src/test/java/com/datavvy/vms/ArchitectureTest.java` (ArchUnit) runs
in CI. CODEOWNERS routes changes of an `api` package to its owning team.
