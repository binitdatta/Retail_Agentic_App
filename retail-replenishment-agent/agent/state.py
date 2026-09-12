"""LangGraph state for one low-stock item's pass through the pipeline.
One instance of this state is built per item returned by
GET /inventory/low-stock and fed through the graph independently — the
per-item fan-out itself lives in runner.py, not in the graph.
"""
from __future__ import annotations

from typing import Any, TypedDict


class ItemState(TypedDict, total=False):
    agent_run_id: int
    dry_run: bool

    # From the low-stock item (see LowStockItemDto)
    store_id: int
    store_code: str
    product_id: int
    sku_code: str
    product_name: str
    is_critical: bool
    on_hand_qty: float
    allocated_qty: float
    available_qty: float
    reorder_point: float
    reorder_qty: float
    safety_stock_qty: float

    # Filled in as the item moves through the graph
    forecast: dict[str, Any]
    suppliers: list[dict[str, Any]]
    order: dict[str, Any] | None
    escalation: dict[str, Any] | None
    error: str | None
