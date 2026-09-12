"""CLI entrypoint: `python -m agent.main [--store-id N] [--dry-run]`

Runs one full detect -> ... -> monitor/escalate cycle and exits. For a
recurring schedule, wrap this in cron / a Kubernetes CronJob / an Azure
Function timer trigger — see the backend's README "Cloud pathway" section
for the target environment.
"""
from __future__ import annotations

import argparse
import logging
import sys

from .config import load_settings
from .runner import run_once


def main() -> int:
    parser = argparse.ArgumentParser(description="Retail replenishment LangGraph agent")
    parser.add_argument("--store-id", type=int, default=None, help="Limit the run to one store (default: all stores)")
    parser.add_argument("--dry-run", action="store_true", help="Run detection/forecast/judgment but skip creating orders or escalations")
    parser.add_argument("--trigger-type", default="MANUAL", choices=["SCHEDULED", "EVENT", "MANUAL"])
    args = parser.parse_args()

    settings = load_settings()
    logging.basicConfig(
        level=getattr(logging, settings.log_level.upper(), logging.INFO),
        format="%(asctime)s %(levelname)-8s %(name)s: %(message)s",
    )

    try:
        run_once(settings, store_id=args.store_id, dry_run=args.dry_run, trigger_type=args.trigger_type)
        return 0
    except Exception:  # noqa: BLE001 — top-level: log and exit non-zero for cron/CI to notice
        logging.exception("Agent run failed")
        return 1


if __name__ == "__main__":
    sys.exit(main())
