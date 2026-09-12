# retail-replenishment-dashboard

Angular 21 standalone-component SPA — agent-run / LLM-cost dashboard for the
retail replenishment POC. Reads from `retail-replenishment-service`'s
`GET /api/agent-runs*` endpoints; nothing here writes to the API.

## Auth

Authorization Code + PKCE against the `retail-replenishment-dashboard`
Keycloak client (public, no secret — see
`retail-replenishment-service/keycloak/retail-replenishment-realm.json`).
Hand-rolled in `core/pkce.util.ts` / `core/auth.service.ts` with the Web
Crypto API rather than a third-party OIDC library, so the whole flow is one
small, auditable file instead of hidden behind a dependency's config
surface. Any of the three realm roles (`REPLENISHMENT_AGENT`,
`SUPPLY_CHAIN_MANAGER`, `DASHBOARD_VIEWER`) can view this dashboard — log
in with `dana.viewer` / `password` (temporary — you'll be asked to set a
real one on first login) for the intended read-only role.

Access token lives in `sessionStorage` — fine for this POC's scope; a
hardened build should move to an in-memory token plus silent-refresh
instead.

## Run

```
npm install
npm start
```

Serves on `http://localhost:4200`. Requires:

- `retail-replenishment-service` running on `http://localhost:8087` with
  CORS configured for `http://localhost:4200` (already set in
  `application.yml: app.cors.allowed-origins` — see the backend project)
- Keycloak running on `http://localhost:8080` with the realm imported
- `http://localhost:4200/*` listed in the Keycloak client's redirect URIs
  (already in the realm export)

Edit `src/environments/environment.ts` if any of those run elsewhere.

## Screens

- **Agent run list** (`/`) — recent runs, status badge, trigger, store,
  timing, summary. Click a row to open it.
- **Agent run detail** (`/runs/:id`) — LLM cost/token/latency/error rollup
  as stat cards, the full decision trace (click a row to expand the raw
  input/output JSON and LLM rationale), and the LLM call log.
- **Orders** (`/orders`, `/orders/:id`) — HITL: approve or cancel a
  `RECOMMENDED` order (requires `SUPPLY_CHAIN_MANAGER`), view line items
  and live delivery/tracking status.
- **Escalations** (`/escalations`) — HITL: acknowledge and resolve
  shortage escalations, with a required resolution note.
- **Inventory** (`/inventory`) — read-only mirror of the low-stock
  detection query.
- **Training** (`/training/*`) — a built-in course companion: architecture,
  MySQL schema, Keycloak, and three-subsection pages for Spring Boot /
  Angular / the Python agent (each with a nested dropdown linking to an
  anchored section), plus a full zero-to-running setup guide. Meant to be
  read from, or recorded from, directly.

## Cloud pathway

`npm run build` produces a static bundle — serve it from anywhere static
(S3+CloudFront, Azure Static Web Apps, nginx in a container). Swap
`environment.prod.ts` for the deployed API/Keycloak URLs; no code changes.
