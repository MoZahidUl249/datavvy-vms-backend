# ADR-0006: Spring Boot 3.5 now, 4.x next

**Status:** accepted · 2026-11-21

## Decision
Start on Spring Boot **3.5.x** (Java 21, Jakarta EE 10, Hibernate 6.6) — the
most widely known baseline for the team. Plan an upgrade to Boot 4.x
(Spring Framework 7, Jackson 3, modular starters) as a dedicated platform-team
task once phase 1 is stable.

## Upgrade notes
Starters are renamed/split in 4.x (`spring-boot-starter-webmvc`,
`spring-boot-starter-flyway`), test slice annotations move packages, and
Jackson moves to `tools.jackson`. Our code uses Jackson only in tests and
avoids test slices, which keeps the upgrade small.
