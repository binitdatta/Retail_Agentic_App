# retail-replenishment-service

Spring Boot 3.5.9 / Java 21 backend for the retail inventory replenishment
agentic POC. Implements the REST surface the LangGraph agent calls at each
of the six pipeline stages, plus the audit trail (agent_run /
agent_decision_log / llm_call_log) that backs the dashboard.

## Prerequisites

- MySQL 8, schema created from `V1__init_retail_replenishment_schema.sql`
  and seeded from `V2__seed_test_data.sql`
- RabbitMQ running locally (`guest`/`guest`, default port)
- Java 21, Maven

`src/main/resources/application.yml` points at `localhost:3306` /
`root`/`localroot`, matching the local lab convention — change before
running against anything else.

## Run

```
mvn spring-boot:run
```

Starts on port 8087. `ddl-auto: validate` — the app will fail fast on
startup if the schema doesn't match the entities exactly, rather than
silently drifting.

## Endpoint map (by pipeline stage)

| Stage                          | Endpoint                                                   |
|---------------------------------|--------------------------------------------------------------|
| Detect low inventory             | `GET /api/inventory/low-stock?storeId=`                     |
| Forecast demand                  | `POST /api/demand-forecasts` (computes + persists), `GET /api/demand-forecasts?storeId=&productId=` (latest) |
| Check supplier availability      | `GET /api/suppliers/availability?productId=`                |
| Recommend/create order           | `POST /api/replenishment-orders`                             |
| Approve / transmit order         | `PATCH /api/replenishment-orders/{id}/status`                |
| Monitor delivery                 | `POST /api/replenishment-orders/{id}/shipments`, `PATCH /api/shipments/{id}/status`, `GET /api/replenishment-orders/{id}/delivery-status` |
| Escalate shortage                | `POST /api/escalations`, `PATCH /api/escalations/{id}/status`, `GET /api/escalations` |

Agent run / audit trail (called by the LangGraph agent, read by the dashboard):

- `POST /api/agent-runs` — start a run, returns `agentRunId`
- `PATCH /api/agent-runs/{id}` — mark completed/failed with a summary
- `POST /api/agent-runs/{id}/decisions` — log one stage decision
- `POST /api/agent-runs/{id}/llm-calls` — log one LLM invocation
- `GET /api/agent-runs/{id}/llm-cost` — cost/token rollup for the run (same shape as `vw_llm_cost_by_run`)

Master data: `GET /api/stores`, `GET /api/products`, `GET /api/suppliers` (+ `/{id}`).

## Notable berollingstoneor

- A shipment status update to `DELIVERED` (`PATCH /api/shipments/{id}/status`)
  receives the ordered quantities into `store_inventory` and closes the
  parent order to `DELIVERED` in the same transaction — no separate
  reconciliation step.
- Every write that matters to the pipeline (order created, delivery status
  changed, shortage escalated) publishes to the `retail.replenishment.events`
  RabbitMQ topic exchange. See `RabbitMqConfig` for the four routing keys
  and their bound queues.
- JSON columns (`agent_decision_log.input_snapshot`/`output_decision`,
  `llm_call_log.request_payload`/`response_payload`) are exposed as real
  nested JSON at the DTO boundary (`Object`), not escaped strings —
  conversion is centralized in `JsonUtil`.

## Security (Keycloak)

The API is an OAuth2 resource server — every endpoint requires a bearer JWT
from Keycloak. Import `keycloak/retail-replenishment-realm.json` into a
running Keycloak instance (`http://localhost:8080` by default) to get:

- **`retail-replenishment-agent`** — confidential client, service accounts
  enabled, `client_credentials` grant only. This is what the LangGraph agent
  authenticates as; its service account carries `REPLENISHMENT_AGENT`.
  Set a real secret before using this anywhere but a local sandbox — the
  placeholder in the export (`CHANGE_ME_LOCAL_DEV_SECRET`) is there so the
  file imports cleanly, not to be trusted.
- **`retail-replenishment-dashboard`** — public client, PKCE (S256) required,
  no secret. For the future dashboard UI.
- Three realm roles: `REPLENISHMENT_AGENT`, `SUPPLY_CHAIN_MANAGER` (demo
  user `priya.manager`), `DASHBOARD_VIEWER` (demo user `dana.viewer`, both
  password `password`, temporary — reset on first login).

Role-to-endpoint mapping (see `SecurityConfig`): the agent's service account
can do everything the pipeline needs autonomously — reads, forecast
computation, order/shipment/escalation creation, the full agent-run audit
trail. Order approval also accepts the agent's own role, since the "critical
SKU → auto-approve" path in `ReplenishmentOrderService` is the agent acting
on its own authority. Escalation *resolution* is `SUPPLY_CHAIN_MANAGER`
only — a human closes those out.

Getting a token for the agent (client_credentials):

```
curl -X POST http://localhost:8080/realms/retail-replenishment/protocol/openid-connect/token \
  -d grant_type=client_credentials \
  -d client_id=retail-replenishment-agent \
  -d client_secret=CHANGE_ME_LOCAL_DEV_SECRET
```

Use the returned `access_token` as `Authorization: Bearer <token>` on every
API call.

## RabbitMQ consumers / delivery simulator

Every write that matters to the pipeline publishes to the
`retail.replenishment.events` topic exchange (see `EventPublisher`), and
each event now has a real consumer in `messaging/`:

- **`SupplierIntegrationStubListener`** (`replenishment.order.created.q`) —
  stands in for a supplier EDI/API integration. A `RECOMMENDED` order is
  left alone (still awaiting `SUPPLY_CHAIN_MANAGER` approval); an
  `APPROVED`/`SENT_TO_SUPPLIER` order gets transmitted, a `DeliveryShipment`
  created, and handed to the delivery simulator. `ReplenishmentOrderService`
  re-publishes to this same queue when an order is approved after the fact,
  so the stub gets a second chance at orders that weren't auto-approved.
- **`DeliverySimulatorService`** — schedules the shipment's
  `DEPARTED → IN_TRANSIT → [DELAYED] → OUT_FOR_DELIVERY → DELIVERED`
  progression over a configurable wall-clock window
  (`app.delivery-simulator.*` in `application.yml`, default ~6 minutes end
  to end — tune to land inside your target 5-10 minute demo run). Each
  scheduled step is a normal call into `DeliveryService`, so `DELIVERED`
  still triggers the real inventory-receipt + order-closing logic — nothing
  about the simulator is special-cased in the business logic it drives.
  `delay-injection-probability` optionally exercises the `DELAYED` path;
  set it to `0` for a guaranteed clean run.
- **`ShortageEscalationNotificationListener`** (`shortage.escalated.q`) —
  formats a Slack/email-style alert, severity-prefixed. Swap the
  `System.out.println` for a real webhook call when ready.
- **`DeliveryStatusAuditListener`** (`delivery.status.updated.q`) — log sink
  today; natural attachment point for a live dashboard feed later.
- **`LowStockDetectedListener`** (`inventory.low-stock.detected.q`) —
  nothing publishes here yet (detection is still agent-polled via
  `GET /api/inventory/low-stock`); wired up as the one-line hook for an
  event-driven agent trigger later, without a new queue/exchange design.

Run RabbitMQ locally and start the app — creating an approved order (or
approving a `RECOMMENDED` one) is enough to see a shipment appear and
progress through delivery on its own.

## Cloud pathway

Containerize as-is; swap `application.yml` for environment-driven config
(`SPRING_DATASOURCE_URL`, etc.), point at Azure Database for MySQL /
Amazon RDS and Azure Service Bus / Amazon MQ in place of local
MySQL/RabbitMQ. No code changes required for that swap — only config.


./sanity_test.sh 'g2bFiVGxkIRQkYkQxcJSIExDh9YnlH1Z'

./write_path_test.sh 'g2bFiVGxkIRQkYkQxcJSIExDh9YnlH1Z'