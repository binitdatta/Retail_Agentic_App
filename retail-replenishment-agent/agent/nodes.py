"""Node implementations for the six-stage pipeline. Each `make_*_node`
factory closes over its dependencies (api client, LLM judgment clients,
settings) and returns a plain `state -> partial_state` callable, which is
what LangGraph expects. Deterministic stages (detect/forecast/check
supplier/monitor) log a decision with no llm_model; judgment stages
(recommend/escalate) log the model and rationale alongside it.
"""
from __future__ import annotations

import logging
import time
from datetime import date, timedelta

from .api_client import ReplenishmentApiClient
from .llm_clients import AnthropicJudgment, parse_json_response
from .state import ItemState

logger = logging.getLogger(__name__)


def _log_http_trace(api: ReplenishmentApiClient, agent_run_id: int, llm_call_id: int | None, result) -> None:
    """Logs the raw wire-level HTTP trace for an LLM call, if one was
    captured (it won't be on a failed call — see AnthropicJudgment). Never
    lets a trace-logging failure interrupt the pipeline; this is
    diagnostic data, not something a run should fail over.
    """
    trace = result.http_trace
    if trace is None or llm_call_id is None:
        return
    try:
        api.log_llm_http_trace(
            agent_run_id=agent_run_id, llm_call_id=llm_call_id,
            http_method=trace.http_method, request_url=trace.request_url,
            request_headers=trace.request_headers, request_body=trace.request_body,
            response_status_code=trace.response_status_code,
            response_headers=trace.response_headers, response_body=trace.response_body,
        )
    except Exception:  # noqa: BLE001 — diagnostic only, never block the pipeline on it
        logger.warning("Could not log HTTP trace for llm_call_id=%s", llm_call_id, exc_info=True)


def make_detect_node(api: ReplenishmentApiClient):
    def detect_low_inventory(state: ItemState) -> dict:
        start = time.perf_counter()
        snapshot = {
            "storeCode": state["store_code"],
            "sku": state["sku_code"],
            "onHandQty": state["on_hand_qty"],
            "allocatedQty": state["allocated_qty"],
            "reorderPoint": state["reorder_point"],
        }
        decision = {
            "flagged": True,
            "availableQty": state["available_qty"],
            "deficit": state["reorder_point"] - state["available_qty"],
        }
        api.log_decision(
            agent_run_id=state["agent_run_id"], stage_name="DETECT_LOW_INVENTORY",
            store_id=state["store_id"], product_id=state["product_id"],
            input_snapshot=snapshot, output_decision=decision,
            duration_ms=int((time.perf_counter() - start) * 1000),
        )
        return {}

    return detect_low_inventory


def make_forecast_node(api: ReplenishmentApiClient, lookback_days: int, horizon_days: int):
    def forecast_demand(state: ItemState) -> dict:
        start = time.perf_counter()
        forecast = api.compute_forecast(
            store_id=state["store_id"], product_id=state["product_id"],
            lookback_days=lookback_days, horizon_days=horizon_days,
            generated_by_agent_run_id=state["agent_run_id"],
        )
        api.log_decision(
            agent_run_id=state["agent_run_id"], stage_name="FORECAST_DEMAND",
            store_id=state["store_id"], product_id=state["product_id"],
            input_snapshot={"sku": state["sku_code"], "lookbackDays": lookback_days, "horizonDays": horizon_days},
            output_decision={
                "forecastMethod": forecast["forecastMethod"],
                "forecastedDemandQty": forecast["forecastedDemandQty"],
                "confidenceScore": forecast["confidenceScore"],
            },
            duration_ms=int((time.perf_counter() - start) * 1000),
        )
        return {"forecast": forecast}

    return forecast_demand


def make_check_supplier_node(api: ReplenishmentApiClient):
    def check_supplier_availability(state: ItemState) -> dict:
        start = time.perf_counter()
        suppliers = api.get_supplier_availability(state["product_id"])
        api.log_decision(
            agent_run_id=state["agent_run_id"], stage_name="CHECK_SUPPLIER_AVAILABILITY",
            store_id=state["store_id"], product_id=state["product_id"],
            input_snapshot={"sku": state["sku_code"]},
            output_decision={
                "available": len(suppliers) > 0,
                "candidateCount": len(suppliers),
                "topChoice": suppliers[0]["supplierCode"] if suppliers else None,
            },
            duration_ms=int((time.perf_counter() - start) * 1000),
        )
        return {"suppliers": suppliers}

    return check_supplier_availability


def route_after_supplier_check(state: ItemState) -> str:
    return "recommend_replenishment" if state.get("suppliers") else "escalate_shortage"


_RECOMMEND_SYSTEM_PROMPT = """You are the ordering-decision component of a retail replenishment agent.
You are given a single SKU's deficit, demand forecast, and its best available supplier.
Decide whether to create a replenishment order, how many units, and whether it qualifies for
auto-approval (skip human review) versus requiring a supply-chain manager's sign-off.

Auto-approval is appropriate when the SKU is business-critical AND the chosen supplier has a
short lead time and high reliability — the cost of a stockout outweighs the cost of skipping
review. Otherwise, recommend the order but leave it for human approval.

Respond with ONLY a JSON object, no other text, in this exact shape:
{"action": "CREATE_ORDER" or "HOLD", "ordered_qty": <number>, "auto_approve": true or false, "rationale": "<one or two sentences>"}
"""


def make_recommend_node(api: ReplenishmentApiClient, judge: AnthropicJudgment, dry_run: bool):
    def recommend_replenishment(state: ItemState) -> dict:
        suppliers = state["suppliers"]
        chosen = suppliers[0]  # backend already sorts preferred-first, then by lead time
        forecast = state["forecast"]
        deficit = state["reorder_point"] - state["available_qty"]

        user_prompt = (
            f"SKU: {state['sku_code']} ({state['product_name']})\n"
            f"Critical SKU: {state['is_critical']}\n"
            f"Deficit (reorder point - available): {deficit}\n"
            f"Forecasted demand ({forecast['forecastHorizonDays']}-day horizon): "
            f"{forecast['forecastedDemandQty']} (confidence {forecast['confidenceScore']})\n"
            f"Safety stock target: {state['safety_stock_qty']}\n"
            f"Default reorder quantity: {state['reorder_qty']}\n"
            f"Chosen supplier: {chosen['supplierCode']} — lead time {chosen['leadTimeDays']}d, "
            f"reliability {chosen['reliabilityScore']}, unit cost {chosen['unitCost']}, "
            f"min order qty {chosen['minOrderQty']}, preferred={chosen['preferred']}"
        )

        llm_start = time.perf_counter()
        result = judge.complete_json(_RECOMMEND_SYSTEM_PROMPT, user_prompt)
        decision = parse_json_response(
            result.text, fallback={"action": "HOLD", "ordered_qty": 0, "auto_approve": False, "rationale": "unparseable LLM response"}
        )

        decision_row = api.log_decision(
            agent_run_id=state["agent_run_id"], stage_name="RECOMMEND_REPLENISHMENT",
            store_id=state["store_id"], product_id=state["product_id"],
            input_snapshot={
                "deficit": deficit, "forecastedDemandQty": forecast["forecastedDemandQty"],
                "safetyStockQty": state["safety_stock_qty"], "reorderQty": state["reorder_qty"],
                "supplierCode": chosen["supplierCode"], "isCritical": state["is_critical"],
            },
            output_decision=decision,
            llm_model=judge.model if result.success else None,
            llm_rationale=decision.get("rationale"),
            duration_ms=int((time.perf_counter() - llm_start) * 1000),
        )

        llm_call_row = api.log_llm_call(
            agent_run_id=state["agent_run_id"], decision_id=decision_row.get("decisionId"),
            provider_code="ANTHROPIC", model_name=judge.model,
            request_payload=result.request_payload, response_payload=result.response_payload,
            prompt_tokens=result.prompt_tokens, completion_tokens=result.completion_tokens,
            estimated_cost_usd=result.estimated_cost_usd, latency_ms=result.latency_ms,
            http_status_code=200 if result.success else 502, success=result.success,
            error_message=result.error_message,
        )
        _log_http_trace(api, state["agent_run_id"], llm_call_row.get("llmCallId"), result)

        if decision.get("action") != "CREATE_ORDER":
            return {"order": None}

        ordered_qty = decision.get("ordered_qty") or state["reorder_qty"]
        ordered_qty = max(float(ordered_qty), float(chosen["minOrderQty"]))

        if dry_run:
            logger.info(
                "[DRY RUN] Would create order for %s x%s from %s (auto_approve=%s)",
                state["sku_code"], ordered_qty, chosen["supplierCode"], decision.get("auto_approve"),
            )
            return {"order": None}

        order_payload = {
            "storeId": state["store_id"],
            "supplierId": chosen["supplierId"],
            "sourceType": "AGENT",
            "generatedByAgentRunId": state["agent_run_id"],
            "requestedDeliveryDate": (date.today() + timedelta(days=chosen["leadTimeDays"])).isoformat(),
            "lines": [{
                "productId": state["product_id"],
                "orderedQty": ordered_qty,
                "unitCost": chosen["unitCost"],
                "forecastRunId": forecast["forecastRunId"],
            }],
        }
        if decision.get("auto_approve"):
            order_payload["approvedBy"] = "replenishment-agent-auto-approval"

        order = api.create_replenishment_order(order_payload)
        return {"order": order}

    return recommend_replenishment


def route_after_recommend(state: ItemState) -> str:
    return "monitor_delivery" if state.get("order") else "__end__"


def make_monitor_delivery_node(api: ReplenishmentApiClient):
    """Logs that the order has been submitted for supplier transmission.
    The actual delivery *progression* is watched by runner.py's post-loop
    monitoring pass, not per-item here — the order was likely just created
    a moment ago and the supplier-integration listener (async, over
    RabbitMQ) hasn't necessarily created a shipment yet.
    """

    def monitor_delivery(state: ItemState) -> dict:
        order = state["order"]
        start = time.perf_counter()
        api.log_decision(
            agent_run_id=state["agent_run_id"], stage_name="MONITOR_DELIVERY",
            store_id=state["store_id"], product_id=state["product_id"],
            input_snapshot={"replenishmentOrderId": order["replenishmentOrderId"], "orderStatus": order["statusCode"]},
            output_decision={"note": "Order submitted; added to this run's delivery watch list."},
            duration_ms=int((time.perf_counter() - start) * 1000),
        )
        return {}

    return monitor_delivery


_ESCALATE_SYSTEM_PROMPT = """You are the shortage-escalation component of a retail replenishment agent.
A SKU is below its reorder point and no supplier is currently available to fulfill it.
Decide the escalation severity a human supply-chain manager should see.

Consider: how business-critical the SKU is, how large the deficit is relative to typical
demand, and (if given) the product's shelf life — a short shelf life compounds urgency because
there's no way to buffer with a larger one-time order once a supplier is found.

Respond with ONLY a JSON object, no other text, in this exact shape:
{"severity": "LOW" or "MEDIUM" or "HIGH" or "CRITICAL", "rationale": "<one or two sentences>"}
"""


def make_escalate_node(api: ReplenishmentApiClient, judge: AnthropicJudgment, dry_run: bool):
    def escalate_shortage(state: ItemState) -> dict:
        deficit = state["reorder_point"] - state["available_qty"]
        shelf_life_days = None
        try:
            product = api.get_product(state["product_id"])
            shelf_life_days = product.get("shelfLifeDays")
        except Exception:  # noqa: BLE001 — enrichment only, never block escalation on it
            logger.warning("Could not fetch product detail for shelf life context; continuing without it")

        user_prompt = (
            f"SKU: {state['sku_code']} ({state['product_name']})\n"
            f"Critical SKU: {state['is_critical']}\n"
            f"Deficit (reorder point - available): {deficit}\n"
            f"Shelf life (days): {shelf_life_days if shelf_life_days is not None else 'unknown'}\n"
            f"Supplier available: false (no supplier_product mapping or none currently reachable)"
        )

        llm_start = time.perf_counter()
        result = judge.complete_json(_ESCALATE_SYSTEM_PROMPT, user_prompt)
        decision = parse_json_response(result.text, fallback={"severity": "MEDIUM", "rationale": "unparseable LLM response"})

        decision_row = api.log_decision(
            agent_run_id=state["agent_run_id"], stage_name="ESCALATE_SHORTAGE",
            store_id=state["store_id"], product_id=state["product_id"],
            input_snapshot={"deficit": deficit, "shelfLifeDays": shelf_life_days, "supplierAvailable": False},
            output_decision={"action": "ESCALATE", "severity": decision.get("severity"), "reason": "SUPPLIER_UNAVAILABLE"},
            llm_model=judge.model if result.success else None,
            llm_rationale=decision.get("rationale"),
            duration_ms=int((time.perf_counter() - llm_start) * 1000),
        )

        llm_call_row = api.log_llm_call(
            agent_run_id=state["agent_run_id"], decision_id=decision_row.get("decisionId"),
            provider_code="ANTHROPIC", model_name=judge.model,
            request_payload=result.request_payload, response_payload=result.response_payload,
            prompt_tokens=result.prompt_tokens, completion_tokens=result.completion_tokens,
            estimated_cost_usd=result.estimated_cost_usd, latency_ms=result.latency_ms,
            http_status_code=200 if result.success else 502, success=result.success,
            error_message=result.error_message,
        )
        _log_http_trace(api, state["agent_run_id"], llm_call_row.get("llmCallId"), result)

        if dry_run:
            logger.info("[DRY RUN] Would escalate %s at severity %s", state["sku_code"], decision.get("severity"))
            return {"escalation": None}

        escalation = api.create_escalation({
            "storeId": state["store_id"],
            "productId": state["product_id"],
            "escalationReason": "SUPPLIER_UNAVAILABLE",
            "severity": decision.get("severity", "MEDIUM"),
            "raisedByAgentRunId": state["agent_run_id"],
        })
        return {"escalation": escalation}

    return escalate_shortage
