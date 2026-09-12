"""Typed(ish) wrapper around the retail-replenishment-service REST API.
Every method here maps to exactly one endpoint documented in that service's
README — no business logic lives here, only HTTP + auth plumbing, so the
graph nodes stay readable.
"""
from __future__ import annotations

import logging
from typing import Any

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from .auth import KeycloakTokenProvider
from .config import Settings

logger = logging.getLogger(__name__)


class ApiError(RuntimeError):
    def __init__(self, method: str, url: str, status_code: int, body: str):
        super().__init__(f"{method} {url} -> {status_code}: {body}")
        self.status_code = status_code
        self.body = body


class ReplenishmentApiClient:
    def __init__(self, settings: Settings, token_provider: KeycloakTokenProvider):
        self._base_url = settings.api_base_url.rstrip("/")
        self._token_provider = token_provider
        self._session = requests.Session()
        # Retries only apply to GET (idempotent) — a POST retry could double-create
        # an order or escalation, which is worse than a single failed request.
        retry = Retry(total=3, backoff_factor=0.5, status_forcelist=[502, 503, 504], allowed_methods=["GET"])
        self._session.mount("http://", HTTPAdapter(max_retries=retry))
        self._session.mount("https://", HTTPAdapter(max_retries=retry))

    # -- internal -----------------------------------------------------

    def _request(self, method: str, path: str, **kwargs) -> Any:
        url = f"{self._base_url}{path}"
        headers = kwargs.pop("headers", {})
        headers["Authorization"] = f"Bearer {self._token_provider.get_token()}"
        response = self._session.request(method, url, headers=headers, timeout=15, **kwargs)
        if not response.ok:
            raise ApiError(method, url, response.status_code, response.text)
        if response.status_code == 204 or not response.content:
            return None
        return response.json()

    # -- inventory / forecast / supplier availability -----------------

    def get_low_stock(self, store_id: int | None = None) -> list[dict]:
        params = {"storeId": store_id} if store_id is not None else {}
        return self._request("GET", "/inventory/low-stock", params=params)

    def get_product(self, product_id: int) -> dict:
        return self._request("GET", f"/products/{product_id}")

    def compute_forecast(
        self, store_id: int, product_id: int, lookback_days: int, horizon_days: int, generated_by_agent_run_id: int
    ) -> dict:
        return self._request(
            "POST",
            "/demand-forecasts",
            json={
                "storeId": store_id,
                "productId": product_id,
                "lookbackDays": lookback_days,
                "horizonDays": horizon_days,
                "generatedByAgentRunId": generated_by_agent_run_id,
            },
        )

    def get_supplier_availability(self, product_id: int) -> list[dict]:
        return self._request("GET", "/suppliers/availability", params={"productId": product_id})

    # -- replenishment orders ------------------------------------------

    def create_replenishment_order(self, payload: dict) -> dict:
        return self._request("POST", "/replenishment-orders", json=payload)

    def update_order_status(self, order_id: int, status_code: str, actor_name: str | None = None) -> dict:
        return self._request(
            "PATCH", f"/replenishment-orders/{order_id}/status",
            json={"statusCode": status_code, "actorName": actor_name},
        )

    def get_delivery_status(self, order_id: int) -> list[dict]:
        return self._request("GET", f"/replenishment-orders/{order_id}/delivery-status")

    # -- escalations -----------------------------------------------------

    def create_escalation(self, payload: dict) -> dict:
        return self._request("POST", "/escalations", json=payload)

    # -- agent run / decision / LLM-call audit trail ----------------------

    def start_agent_run(self, trigger_type: str, trigger_source: str | None, store_id: int | None = None) -> dict:
        return self._request(
            "POST", "/agent-runs",
            json={"triggerType": trigger_type, "triggerSource": trigger_source, "storeId": store_id},
        )

    def complete_agent_run(self, agent_run_id: int, status: str, summary: str) -> dict:
        return self._request(
            "PATCH", f"/agent-runs/{agent_run_id}",
            json={"status": status, "summary": summary},
        )

    def log_decision(
        self,
        agent_run_id: int,
        stage_name: str,
        input_snapshot: Any,
        output_decision: Any,
        store_id: int | None = None,
        product_id: int | None = None,
        llm_model: str | None = None,
        llm_rationale: str | None = None,
        duration_ms: int | None = None,
    ) -> dict:
        return self._request(
            "POST", f"/agent-runs/{agent_run_id}/decisions",
            json={
                "stageName": stage_name,
                "storeId": store_id,
                "productId": product_id,
                "inputSnapshot": input_snapshot,
                "outputDecision": output_decision,
                "llmModel": llm_model,
                "llmRationale": llm_rationale,
                "durationMs": duration_ms,
            },
        )

    def log_llm_call(
        self,
        agent_run_id: int,
        decision_id: int | None,
        provider_code: str,
        model_name: str,
        request_payload: Any,
        response_payload: Any,
        prompt_tokens: int | None,
        completion_tokens: int | None,
        estimated_cost_usd: float | None,
        latency_ms: int | None,
        http_status_code: int | None,
        success: bool,
        error_message: str | None = None,
    ) -> dict:
        return self._request(
            "POST", f"/agent-runs/{agent_run_id}/llm-calls",
            json={
                "decisionId": decision_id,
                "providerCode": provider_code,
                "modelName": model_name,
                "requestPayload": request_payload,
                "responsePayload": response_payload,
                "promptTokens": prompt_tokens,
                "completionTokens": completion_tokens,
                "estimatedCostUsd": estimated_cost_usd,
                "latencyMs": latency_ms,
                "httpStatusCode": http_status_code,
                "success": success,
                "errorMessage": error_message,
            },
        )

    def log_llm_http_trace(
        self,
        agent_run_id: int,
        llm_call_id: int,
        http_method: str,
        request_url: str,
        request_headers: dict,
        request_body: Any,
        response_status_code: int | None,
        response_headers: dict | None,
        response_body: Any,
        request_params: dict | None = None,
    ) -> dict:
        """Raw wire-level HTTP capture for one LLM call — separate from
        log_llm_call's logical payload. request_headers must already have
        any credential redacted by the caller (see llm_clients.py) before
        it reaches this method; this client does not re-check that.
        """
        return self._request(
            "POST", f"/agent-runs/{agent_run_id}/llm-calls/{llm_call_id}/http-trace",
            json={
                "httpMethod": http_method,
                "requestUrl": request_url,
                "requestHeaders": request_headers,
                "requestParams": request_params,
                "requestBody": request_body,
                "responseStatusCode": response_status_code,
                "responseHeaders": response_headers,
                "responseBody": response_body,
            },
        )

