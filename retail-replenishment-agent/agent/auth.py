"""Keycloak client_credentials token acquisition for the agent's own
service-account identity (the retail-replenishment-agent confidential
client — see keycloak/retail-replenishment-realm.json in the backend repo).
No user is ever involved in this flow.
"""
from __future__ import annotations

import logging
import time

import requests

from .config import Settings

logger = logging.getLogger(__name__)

# Refresh this many seconds before actual expiry, so a token never goes
# stale mid-request due to clock drift or network latency at the edge.
_EXPIRY_SAFETY_MARGIN_SECONDS = 30


class KeycloakTokenProvider:
    def __init__(self, settings: Settings):
        self._settings = settings
        self._token_endpoint = f"{settings.keycloak_issuer}/protocol/openid-connect/token"
        self._access_token: str | None = None
        self._expires_at: float = 0.0

    def get_token(self) -> str:
        if self._access_token and time.monotonic() < self._expires_at:
            return self._access_token
        return self._fetch_token()

    def _fetch_token(self) -> str:
        logger.info("Fetching a new access token from %s", self._token_endpoint)
        response = requests.post(
            self._token_endpoint,
            data={
                "grant_type": "client_credentials",
                "client_id": self._settings.keycloak_client_id,
                "client_secret": self._settings.keycloak_client_secret,
            },
            timeout=10,
        )
        response.raise_for_status()
        payload = response.json()

        self._access_token = payload["access_token"]
        expires_in = payload.get("expires_in", 300)
        self._expires_at = time.monotonic() + expires_in - _EXPIRY_SAFETY_MARGIN_SECONDS
        return self._access_token
