"""Wires the six-stage pipeline into a LangGraph StateGraph.

    detect_low_inventory -> forecast_demand -> check_supplier_availability
        -> (supplier found)    -> recommend_replenishment -> (order created) -> monitor_delivery -> END
                                                            -> (no order)     -> END
        -> (no supplier found) -> escalate_shortage -> END

detect/forecast/check/monitor are deterministic (no LLM); recommend and
escalate are the two judgment calls, both backed by Anthropic.
"""
from __future__ import annotations

from langgraph.graph import END, StateGraph

from .api_client import ReplenishmentApiClient
from .config import Settings
from .llm_clients import AnthropicJudgment
from .nodes import (
    make_check_supplier_node,
    make_detect_node,
    make_escalate_node,
    make_forecast_node,
    make_monitor_delivery_node,
    make_recommend_node,
    route_after_recommend,
    route_after_supplier_check,
)
from .state import ItemState


def build_graph(
    api: ReplenishmentApiClient,
    anthropic_judge: AnthropicJudgment,
    settings: Settings,
    dry_run: bool = False,
):
    graph = StateGraph(ItemState)

    graph.add_node("detect_low_inventory", make_detect_node(api))
    graph.add_node(
        "forecast_demand",
        make_forecast_node(api, settings.forecast_lookback_days, settings.forecast_horizon_days),
    )
    graph.add_node("check_supplier_availability", make_check_supplier_node(api))
    graph.add_node("recommend_replenishment", make_recommend_node(api, anthropic_judge, dry_run))
    graph.add_node("monitor_delivery", make_monitor_delivery_node(api))
    graph.add_node("escalate_shortage", make_escalate_node(api, anthropic_judge, dry_run))

    graph.set_entry_point("detect_low_inventory")
    graph.add_edge("detect_low_inventory", "forecast_demand")
    graph.add_edge("forecast_demand", "check_supplier_availability")

    graph.add_conditional_edges(
        "check_supplier_availability",
        route_after_supplier_check,
        {"recommend_replenishment": "recommend_replenishment", "escalate_shortage": "escalate_shortage"},
    )
    graph.add_conditional_edges(
        "recommend_replenishment",
        route_after_recommend,
        {"monitor_delivery": "monitor_delivery", "__end__": END},
    )
    graph.add_edge("monitor_delivery", END)
    graph.add_edge("escalate_shortage", END)

    return graph.compile()