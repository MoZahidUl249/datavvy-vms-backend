# ADR-0001: Modular monolith

**Status:** accepted · 2026-11-21

## Context
Several teams will build VMS capabilities (fleet, maintenance, fuel, drivers,
trips). We need parallel work and clear ownership now, without the operational
cost of microservices (service discovery, distributed transactions, many pipelines).

## Decision
One Spring Boot deployable (`vms-app`) assembled from Maven modules, one per
bounded context, plus a tiny shared kernel (`vms-platform`). Same idea on the
client: one Flutter Web app assembled from workspace packages.

## Consequences
+ One build, one deploy, one DB transaction when needed; easy local setup.
+ Boundaries enforced by Maven dependencies and `ArchitectureTest`.
+ Clear extraction path: a module already has its own API, tables and owners.
− A bad release of one module ships with all others → mitigated by module
  tests, CODEOWNERS reviews and feature flags when needed.
