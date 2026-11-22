# Team ownership

| Team | Backend | Frontend | Tables |
|---|---|---|---|
| Platform | `vms-platform`, `vms-auth`, `vms-app` (with backend leads) | `vms_core`, `feature_auth` | `app_user` |
| Design system | — | `vms_design_system` | — |
| Fleet Core | `vms-vehicle` | `feature_vehicles` | `vehicle` |
| Fleet Settings | `vms-catalog` | `vms_catalog`, `feature_settings` | `catalog_item` |
| Insights | `vms-dashboard` | `feature_dashboard` | — |
| Architecture group | `AGENTS.md`, `.agents/`, `docs/` | | |

Contract (`contracts/`) changes need a backend **and** a frontend lead.
GitHub enforces this through `.github/CODEOWNERS` (team names are placeholders).

## Working in parallel

- One branch (and one agent) per module per task.
- Shared-module changes go in their own small PR first, then features build on them.
- Migrations use timestamp versions, so two teams never pick the same number.
