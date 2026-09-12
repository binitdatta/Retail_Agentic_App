"""Orchestrates one full pipeline run: start an agent_run, fan out over
every low-stock item through the graph, then stay alive watching delivery
progress on whatever orders it just created — landing the whole process in
the 5-10 minute window the POC calls for, without the graph itself needing
to know about wall-clock pacing.
"""
from __future__ import annotations

import logging
import time
from datetime import datetime

from .api_client import ApiError, ReplenishmentApiClient
from .auth import KeycloakTokenProvider
from .config import Settings
from .graph import build_graph
from .llm_clients import AnthropicJudgment
from .state import ItemState

logger = logging.getLogger(__name__)

_TERMINAL_SHIPMENT_STATUSES = {"DELIVERED", "EXCEPTION"}


def _item_state_from_dto(item: dict, agent_run_id: int, dry_run: bool) -> ItemState:
    return ItemState(
        agent_run_id=agent_run_id,
        dry_run=dry_run,
        store_id=item["storeId"],
        store_code=item["storeCode"],
        product_id=item["productId"],
        sku_code=item["skuCode"],
        product_name=item["productName"],
        is_critical=bool(item["critical"]),
        on_hand_qty=item["onHandQty"],
        allocated_qty=item["allocatedQty"],
        available_qty=item["availableQty"],
        reorder_point=item["reorderPoint"],
        reorder_qty=item["reorderQty"],
        safety_stock_qty=item["safetyStockQty"],
    )


def run_once(settings: Settings, store_id: int | None = None, dry_run: bool = False, trigger_type: str = "SCHEDULED") -> str:
    token_provider = KeycloakTokenProvider(settings)
    api = ReplenishmentApiClient(settings, token_provider)
    anthropic_judge = AnthropicJudgment(settings)
    graph = build_graph(api, anthropic_judge, settings, dry_run=dry_run)

    run = api.start_agent_run(trigger_type=trigger_type, trigger_source="langgraph-agent-cli", store_id=store_id)
    agent_run_id = run["agentRunId"]
    logger.info("Started agent_run %s (uuid=%s)", agent_run_id, run["runUuid"])

    orders_created = 0
    orders_auto_approved = 0
    escalations_raised = 0
    items_scanned = 0
    errors: list[str] = []
    watch_list: dict[int, dict] = {}  # replenishmentOrderId -> {"statusCode": str, "storeId": int, "productId": int}

    try:
        items = api.get_low_stock(store_id)
        items_scanned = len(items)
        logger.info("Detected %s low-stock item(s)%s", items_scanned, f" for store {store_id}" if store_id else "")

        for item in items:
            state = _item_state_from_dto(item, agent_run_id, dry_run)
            try:
                result = graph.invoke(state)
            except Exception as exc:  # noqa: BLE001 — one bad item should not kill the whole run
                logger.exception("Item %s / %s failed", item["storeCode"], item["skuCode"])
                errors.append(f"{item['storeCode']}/{item['skuCode']}: {exc}")
                continue

            order = result.get("order")
            if order:
                orders_created += 1
                if order["statusCode"] == "APPROVED":
                    orders_auto_approved += 1
                watch_list[order["replenishmentOrderId"]] = {
                    "statusCode": None,
                    "storeId": state["store_id"],
                    "productId": state["product_id"],
                }

            escalation = result.get("escalation")
            if escalation:
                escalations_raised += 1

        if watch_list and not dry_run:
            _monitor_deliveries(api, agent_run_id, watch_list, settings)

        summary = (
            f"Scanned {items_scanned} low-stock item(s). "
            f"{orders_created} order(s) recommended ({orders_auto_approved} auto-approved). "
            f"{escalations_raised} shortage escalation(s) raised."
        )
        if errors:
            summary += f" {len(errors)} item(s) failed: " + "; ".join(errors[:5])

        api.complete_agent_run(agent_run_id, status="COMPLETED", summary=summary)
        logger.info("agent_run %s complete: %s", agent_run_id, summary)
        return summary

    except Exception as exc:  # noqa: BLE001 — make sure a crash still closes out the run record
        logger.exception("agent_run %s failed", agent_run_id)
        try:
            api.complete_agent_run(agent_run_id, status="FAILED", summary=f"Run failed: {exc}")
        except Exception:  # noqa: BLE001
            logger.exception("Additionally failed to mark agent_run %s as FAILED", agent_run_id)
        raise


def _monitor_deliveries(api: ReplenishmentApiClient, agent_run_id: int, watch_list: dict[int, dict], settings: Settings) -> None:
    deadline = time.monotonic() + settings.monitor_delivery_max_minutes * 60
    logger.info(
        "Monitoring delivery for %s order(s) for up to %s minute(s)",
        len(watch_list), settings.monitor_delivery_max_minutes,
    )

    while time.monotonic() < deadline:
        all_terminal = True
        for order_id, watch in watch_list.items():
            try:
                shipments = api.get_delivery_status(order_id)
            except ApiError as exc:
                if exc.status_code == 404:
                    # No shipment created yet — the supplier-integration
                    # listener hasn't processed the order.created event yet.
                    all_terminal = False
                    continue
                raise

            if not shipments:
                all_terminal = False
                continue

            latest = shipments[0]  # backend orders shipments by shipmentId DESC
            current_status = latest["statusCode"]
            if current_status != watch["statusCode"]:
                api.log_decision(
                    agent_run_id=agent_run_id, stage_name="MONITOR_DELIVERY",
                    store_id=watch["storeId"], product_id=watch["productId"],
                    input_snapshot={"replenishmentOrderId": order_id, "shipmentId": latest["shipmentId"]},
                    output_decision={"statusCode": current_status, "observedAt": datetime.now().isoformat()},
                )
                watch["statusCode"] = current_status
                logger.info("Order %s shipment now %s", order_id, current_status)

            if current_status not in _TERMINAL_SHIPMENT_STATUSES:
                all_terminal = False

        if all_terminal:
            logger.info("All watched shipments reached a terminal status — ending monitoring early")
            return

        time.sleep(settings.monitor_delivery_poll_seconds)

    logger.info("Monitoring window elapsed with %s order(s) still not delivered", len(watch_list))