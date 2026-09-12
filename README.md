# Retail Agentic App

**A governed, fully-auditable agentic AI reference architecture for retail inventory replenishment.**

A LangGraph agent autonomously detects low stock, forecasts demand, checks supplier availability, and either
recommends a replenishment order or escalates a shortage — with every deterministic step and every LLM judgment
call logged, costed, and reviewable. Human approval is enforced structurally, not by convention: a Spring Boot
API backed by Keycloak roles decides what the agent may do on its own authority versus what a supply chain
manager must approve, and an Angular dashboard is where that approval actually happens.

This repository is a proof-of-concept reference implementation, not a production system. See
[Status and known limitations](#status-and-known-limitations) before using any part of it against real data.

---

## Contents

- [Architecture](#architecture)
- [Repository structure](#repository-structure)
- [Quick start](#quick-start)
- [What this demonstrates](#what-this-demonstrates)
- [Tech stack](#tech-stack)
- [Documentation](#documentation)
- [Status and known limitations](#status-and-known-limitations)
- [License](#license)

## Architecture

Five components, each with one responsibility:

| Component | Role |
|---|---|
| **Keycloak** | The sole identity provider. Issues short-lived, role-carrying JWTs. Nothing else implements its own login logic. |
| **Angular dashboard** | Where a human reviews agent activity and takes human-in-the-loop (HITL) action — approve/cancel a recommended order, acknowledge/resolve an escalation. Authenticates via Authorization Code + PKCE. |
| **Python (LangGraph) agent** | The autonomous worker. Authenticates as its own service identity (`client_credentials`), runs the six-stage pipeline, calls an LLM only for the two genuine judgment calls. |
| **Spring Boot API** | The single point of business logic and authorization. Every request — human or agent — is validated and role-checked here. |
| **MySQL 8** | System of record: inventory, orders, escalations, and the complete decision + LLM-cost audit trail. |

RabbitMQ decouples slower downstream effects (transmitting an order to a supplier, simulating shipment
progress) from the request/response path, so those concerns evolve independently of the API.

```
Angular dashboard ──┐                         ┌── MySQL 8 (system of record)
  (human, PKCE)      ├──► Spring Boot API ◄────┤
Python agent ────────┘     (OAuth2 resource     └── RabbitMQ (order/delivery/escalation events)
  (service, client_        server, all business
   credentials)            logic + authZ)
        │
        └──► Anthropic (the two judgment calls: recommend_replenishment, escalate_shortage)

Keycloak issues every token above and is the only thing either client authenticates against.
```

A full interactive version of this diagram, with click-through explanations of each component, is in the
dashboard's built-in **Training** section once the app is running (see [Documentation](#documentation)).

## Repository structure

```
Retail_Agentic_App/
├── V1__init_retail_replenishment_schema.sql   # DBA-owned DDL — 21 tables, 2 views
├── V2__seed_test_data.sql                     # Demo dataset: 3 stores, 10 SKUs, 5 low-stock scenarios
├── retail-replenishment-service/              # Spring Boot 3.5 REST API (Java 21, Hibernate 6)
│   └── keycloak/retail-replenishment-realm.json   # Realm export: clients, roles, demo users
├── retail-replenishment-dashboard/             # Angular 21 SPA — operator dashboard + Training section
└── retail-replenishment-agent/                 # Python LangGraph agent (CLI, not a web service)
```

Each sub-project has its own README with setup and run instructions specific to that layer.

## Quick start

Full step-by-step instructions — including a Homebrew-based Mac install for every prerequisite, and an
end-to-end confidence checklist — are built into the running app at **Training → Setup guide**. Summary:

1. **Database**: run `V1__init_retail_replenishment_schema.sql`, then `V2__seed_test_data.sql`, in MySQL
   Workbench, against a local MySQL 8 instance.
2. **Keycloak**: import `retail-replenishment-service/keycloak/retail-replenishment-realm.json` into a running
   Keycloak instance to get the realm, its two OAuth2 clients, three roles, and demo users.
3. **Backend** (`retail-replenishment-service`): `mvn clean install`, run the jar. Serves on `:8087`.
4. **Dashboard** (`retail-replenishment-dashboard`): `npm install`, `npm start`. Serves on `:4200`.
5. **Agent** (`retail-replenishment-agent`): `pip install -r requirements.txt`, copy `.env.example` to `.env`
   and fill in your Keycloak client secret and Anthropic API key, then `python -m agent.main --dry-run` to
   verify before a real run.

Prerequisites: Java 21, Maven, MySQL 8 + MySQL Workbench, Keycloak (standalone distribution), RabbitMQ,
Node.js + npm, Python 3.12, and an Anthropic API key.

## What this demonstrates

- **A complete, queryable audit trail for AI-assisted decisions** — not just outcomes. Every pipeline stage logs
  its inputs and outputs; every LLM call logs its model, full request/response payload, token counts, latency,
  and cost. A separate raw-HTTP-trace layer captures the literal wire-level request/response (redacted of any
  credential) for the two judgment calls, distinct from the reconstructed logical payload.
- **Structural human-in-the-loop governance** — approval authority is enforced by Keycloak roles checked at the
  API layer, not a convention an agent (or a person) could route around. Auto-approval is narrowly scoped:
  critical SKU + reliable, short-lead-time supplier only.
- **Event-driven side effects** — order transmission and delivery simulation run off RabbitMQ events, decoupled
  from the request that triggered them.
- **A provider-neutral judgment layer** — the LLM call is isolated behind a narrow interface in the agent; the
  two judgment calls (order recommendation, escalation severity) can run on a different provider without
  touching the surrounding pipeline.

## Tech stack

| Layer | Stack |
|---|---|
| Backend | Spring Boot 3.5.9, Java 21, Hibernate 6, Spring Security (OAuth2 resource server), Spring Data JPA, Spring AMQP |
| Database | MySQL 8 |
| Identity | Keycloak (OAuth2 / OIDC — Authorization Code + PKCE for the dashboard, client_credentials for the agent) |
| Messaging | RabbitMQ |
| Dashboard | Angular 21 (standalone components), Bootstrap 5 |
| Agent | Python 3.12, LangGraph, Anthropic SDK |

## Documentation

The primary documentation for this project is **inside the running dashboard**, under the **Training** menu —
it covers the architecture, the database schema, Keycloak configuration, each sub-project's internals, a full
setup guide, a real annotated demo-run log walkthrough, and the business case driving this build. Each
sub-project's own `README.md` covers that layer's specific setup and run commands.

## Status and known limitations

This is a proof of concept, exercised end to end against a synthetic demo dataset — not a production system.
Before using any part of this against real data or a real supplier:

- Supplier transmission is simulated (`SupplierIntegrationStubListener`), not a real EDI/API integration.
- Delivery progress is simulated on a fixed wall-clock cadence, not driven by real carrier data.
- No automated test suite yet — validated through manual end-to-end runs.
- Financial ROI is deliberately left unquantified in the business case pending real baseline data — see the
  Training → Business case page.
- Escalations for a SKU with no supplier mapping are not deduplicated — repeated agent runs on an unresolved
  shortage will raise a new escalation each time rather than recognizing an existing open one.

## License

No license file is currently included. Until one is added, this repository is all-rights-reserved by default
under GitHub's terms — add a `LICENSE` file with your chosen license before treating this as open for reuse.
