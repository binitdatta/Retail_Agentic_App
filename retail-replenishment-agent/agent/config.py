"""Environment-driven settings — no hardcoded credentials or URLs anywhere
else in the codebase. Mirrors the local-dev defaults baked into the Spring
Boot service's application.yml and the Keycloak realm export, so a fresh
checkout with a copied .env works against the local stack out of the box.
"""
from __future__ import annotations

import os
from dataclasses import dataclass

from dotenv import load_dotenv

load_dotenv()


def _env(name: str, default: str | None = None, required: bool = False) -> str:
    value = os.environ.get(name, default)
    if required and not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value or ""


@dataclass(frozen=True)
class Settings:
    api_base_url: str
    keycloak_issuer: str
    keycloak_client_id: str
    keycloak_client_secret: str

    anthropic_api_key: str
    anthropic_model: str

    forecast_lookback_days: int
    forecast_horizon_days: int

    monitor_delivery_max_minutes: int
    monitor_delivery_poll_seconds: int

    log_level: str


def load_settings() -> Settings:
    return Settings(
        api_base_url=_env("API_BASE_URL", "http://localhost:8087/api"),
        keycloak_issuer=_env("KEYCLOAK_ISSUER", "http://localhost:8080/realms/retail-replenishment"),
        keycloak_client_id=_env("KEYCLOAK_CLIENT_ID", "retail-replenishment-agent"),
        keycloak_client_secret=_env("KEYCLOAK_CLIENT_SECRET", required=True),
        anthropic_api_key=_env("ANTHROPIC_API_KEY", required=True),
        anthropic_model=_env("ANTHROPIC_MODEL", "claude-sonnet-4-6"),
        forecast_lookback_days=int(_env("FORECAST_LOOKBACK_DAYS", "14")),
        forecast_horizon_days=int(_env("FORECAST_HORIZON_DAYS", "7")),
        monitor_delivery_max_minutes=int(_env("MONITOR_DELIVERY_MAX_MINUTES", "8")),
        monitor_delivery_poll_seconds=int(_env("MONITOR_DELIVERY_POLL_SECONDS", "30")),
        log_level=_env("LOG_LEVEL", "INFO"),
    )