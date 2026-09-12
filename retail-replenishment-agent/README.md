# retail-replenishment-agent

LangGraph agent for the retail replenishment POC. Calls
`retail-replenishment-service`'s REST API as the `retail-replenishment-agent`
Keycloak service account (client_credentials — no user involved), and stays
alive watching delivery progress until the run's monitoring window elapses
or every shipment it created reaches a terminal status.

## Pipeline

```
detect_low_inventory -> forecast_demand -> check_supplier_availability
    --(supplier found)--> recommend_replenishment --(order created)--> monitor_delivery -> END
                                                   --(no order)------> END
    --(no supplier)-----> escalate_shortage -> END
```

Deterministic stages (detect / forecast / check supplier / monitor) do no
LLM call — they're arithmetic and lookups, logged to `agent_decision_log`
with no `llm_model`. The two judgment calls:

- **`recommend_replenishment`** — Anthropic (`ANTHROPIC_MODEL`). Given the
  deficit, forecast, and best available supplier, decides whether to order,
  how much, and whether it qualifies for auto-approval.
- **`escalate_shortage`** — Azure AI Foundry / Llama (`AZURE_AI_FOUNDRY_MODEL`).
  Given the deficit and (when available) the SKU's shelf life, decides
  escalation severity.

Both log an `llm_call_log` row with the full request/response payload,
regardless of provider — see `llm_clients.py`'s shared `LlmCallResult`.

## Setup

```
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env   # fill in KEYCLOAK_CLIENT_SECRET, ANTHROPIC_API_KEY, AZURE_AI_FOUNDRY_*
```

Requires `retail-replenishment-service` running (with its schema loaded)
and Keycloak running with the realm imported — see that project's README.

## Run

```
python -m agent.main                    # all stores, live (creates real orders/escalations)
python -m agent.main --store-id 1       # one store only
python -m agent.main --dry-run          # exercises detection/forecast/judgment, skips order/escalation POSTs
```

One invocation = one full cycle: detect across every low-stock item, run
each through the graph, then monitor delivery on whatever orders it just
created for up to `MONITOR_DELIVERY_MAX_MINUTES` (default 8 — comfortably
longer than the backend delivery-simulator's ~6 minute default cadence, so
the agent is still watching when `DELIVERED` events land). For a recurring
schedule, wrap this in cron / a Kubernetes CronJob / a cloud function timer
trigger rather than looping inside the process.

## What's deliberately NOT here

- **Retry-into-duplicate risk**: POSTs (order/escalation creation) are never
  retried automatically — a transient failure fails that one item and moves
  on (see the per-item `try/except` in `runner.py`), rather than risking a
  duplicate order from a retried non-idempotent call.
- **A persistent worker loop**: this is a batch CLI, not a long-running
  service with its own scheduler. Cloud pathway: containerize as-is and let
  the orchestrator (cron/K8s/cloud scheduler) own the cadence.
- **LLM-authored run summaries**: `runner.py` builds the `agent_run.summary`
  deterministically from counts, not via an extra LLM call — one less
  point of failure/cost for a string that's easy to construct directly.

## Cloud pathway

Containerize (`Dockerfile` not included here — same pattern as the other
POC services: multi-stage build, `python:3.12-slim` base). Point
`KEYCLOAK_ISSUER` / `API_BASE_URL` / provider keys at their deployed
equivalents via environment variables — no code changes. Trigger via a
cloud scheduler (Azure Container Apps Jobs on a cron trigger, or an AWS
Lambda/Fargate task on EventBridge) instead of a local cron entry.

``` 
(.venv) binit.datta@C6NWKQ290Y retail-replenishment-agent % python -m agent.main
/Users/binit.datta/Development/OpenSource/Retail_Agentic_App/retail-replenishment-agent/.venv/lib/python3.12/site-packages/langgraph/checkpoint/base/__init__.py:18: LangChainPendingDeprecationWarning: The default value of `allowed_objects` will change in a future version. Pass an explicit value (e.g., allowed_objects='messages' or allowed_objects='core') to suppress this warning.
  from langgraph.checkpoint.serde.jsonplus import JsonPlusSerializer
2026-09-12 07:00:31,932 INFO     agent.auth: Fetching a new access token from http://localhost:8080/realms/retail-replenishment/protocol/openid-connect/token
2026-09-12 07:00:31,972 INFO     agent.runner: Started agent_run 1 (uuid=911659a1-24a8-4265-b638-71e473fbdb0f)
2026-09-12 07:00:31,979 INFO     agent.runner: Detected 5 low-stock item(s)
2026-09-12 07:00:38,513 INFO     httpx: HTTP Request: POST https://api.anthropic.com/v1/messages "HTTP/1.1 200 OK"
2026-09-12 07:00:48,205 INFO     httpx: HTTP Request: POST https://api.anthropic.com/v1/messages "HTTP/1.1 200 OK"
2026-09-12 07:00:54,385 INFO     httpx: HTTP Request: POST https://api.anthropic.com/v1/messages "HTTP/1.1 200 OK"
2026-09-12 07:01:04,318 INFO     httpx: HTTP Request: POST https://api.anthropic.com/v1/messages "HTTP/1.1 200 OK"
2026-09-12 07:01:06,777 INFO     httpx: HTTP Request: POST https://api.anthropic.com/v1/messages "HTTP/1.1 200 OK"
2026-09-12 07:01:06,840 INFO     agent.runner: Monitoring delivery for 4 order(s) for up to 8 minute(s)
2026-09-12 07:01:06,858 INFO     agent.runner: Order 1 shipment now CREATED
2026-09-12 07:01:36,907 INFO     agent.runner: Order 1 shipment now DEPARTED
2026-09-12 07:02:37,001 INFO     agent.runner: Order 1 shipment now IN_TRANSIT
2026-09-12 07:05:07,261 INFO     agent.runner: Order 1 shipment now OUT_FOR_DELIVERY
2026-09-12 07:06:07,379 INFO     agent.runner: Order 2 shipment now CREATED
2026-09-12 07:06:07,401 INFO     agent.runner: Order 3 shipment now CREATED
2026-09-12 07:06:07,419 INFO     agent.runner: Order 4 shipment now CREATED
2026-09-12 07:06:37,484 INFO     agent.runner: Order 2 shipment now DEPARTED
2026-09-12 07:06:37,510 INFO     agent.runner: Order 3 shipment now DEPARTED
2026-09-12 07:06:37,529 INFO     agent.runner: Order 4 shipment now DEPARTED
2026-09-12 07:07:07,567 INFO     agent.runner: Order 1 shipment now DELIVERED
2026-09-12 07:07:37,631 INFO     agent.runner: Order 2 shipment now IN_TRANSIT
2026-09-12 07:07:37,644 INFO     agent.runner: Order 3 shipment now IN_TRANSIT
2026-09-12 07:07:37,656 INFO     agent.runner: Order 4 shipment now IN_TRANSIT


cd retail-replenishment-agent
unzip -o ../agent-http-trace-feature.zip
```