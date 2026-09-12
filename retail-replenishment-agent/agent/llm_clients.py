"""LLM client wrapper returning a uniform LlmCallResult shape, so nodes.py
can log an llm_call_log row identically regardless of which pipeline stage
made the call. Pricing is illustrative — update PRICING to your actual
contracted rates before trusting estimated_cost_usd for anything real.

LlmCallResult also carries the *raw wire-level* HTTP detail (URL, method,
headers, literal request/response bodies) captured via the Anthropic SDK's
`.with_raw_response` mode — this is genuinely what went over the wire, not
a reconstruction of what the code thinks it sent. Any credential header is
redacted before it ever leaves this file; see `_redact_headers`.

NOTE ON SDK COMPATIBILITY: `.with_raw_response` and the shape of the
returned httpx Request/Response objects are stable in recent `anthropic`
SDK versions, but this hasn't been exercised against your installed
version in this environment — if attribute access here throws, the fix is
almost always a small rename (e.g. `.http_response` vs `.response`), not a
redesign. Run it once and check the traceback if so.
"""
from __future__ import annotations

import json
import logging
import time
from dataclasses import dataclass
from typing import Any

from anthropic import Anthropic

from .config import Settings

logger = logging.getLogger(__name__)

# USD per 1K tokens (input, output). Illustrative — Anthropic list pricing
# at time of writing for Sonnet-class models. Update before trusting
# estimated_cost_usd for anything real.
PRICING = {
    "claude-sonnet-4-6": (0.003, 0.015),
}

# Header names (case-insensitive) that must never reach the database with
# their real value. Redacted to a fixed literal, never partially masked —
# partial masking ("partially-masked-api-key") still leaks enough to be dangerous.
_SENSITIVE_HEADER_NAMES = {"x-api-key", "authorization"}
_REDACTED = "REDACTED"


def _redact_headers(headers: dict[str, str]) -> dict[str, str]:
    return {
        k: (_REDACTED if k.lower() in _SENSITIVE_HEADER_NAMES else v)
        for k, v in headers.items()
    }


def _safe_json(raw_bytes_or_text: Any) -> Any:
    """Best-effort parse of a raw HTTP body into JSON for storage; falls
    back to the raw text if it isn't valid JSON (e.g. an error page),
    rather than dropping the body entirely.
    """
    if raw_bytes_or_text is None:
        return None
    text = raw_bytes_or_text.decode("utf-8", errors="replace") if isinstance(raw_bytes_or_text, (bytes, bytearray)) else str(raw_bytes_or_text)
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return {"_raw_text": text}


@dataclass
class HttpTrace:
    http_method: str
    request_url: str
    request_headers: dict[str, str]
    request_body: Any
    response_status_code: int | None
    response_headers: dict[str, str] | None
    response_body: Any


@dataclass
class LlmCallResult:
    text: str
    prompt_tokens: int | None
    completion_tokens: int | None
    estimated_cost_usd: float | None
    latency_ms: int
    request_payload: Any
    response_payload: Any
    success: bool
    error_message: str | None = None
    http_trace: HttpTrace | None = None


def _estimate_cost(model: str, prompt_tokens: int | None, completion_tokens: int | None) -> float | None:
    rates = PRICING.get(model)
    if not rates or prompt_tokens is None or completion_tokens is None:
        return None
    input_rate, output_rate = rates
    return round((prompt_tokens / 1000) * input_rate + (completion_tokens / 1000) * output_rate, 6)


class AnthropicJudgment:
    """Backs recommend_replenishment — the order/auto-approve judgment call."""

    def __init__(self, settings: Settings):
        self._client = Anthropic(api_key=settings.anthropic_api_key)
        self._model = settings.anthropic_model

    @property
    def model(self) -> str:
        return self._model

    def complete_json(self, system_prompt: str, user_prompt: str, max_tokens: int = 500) -> LlmCallResult:
        request_payload = {
            "model": self._model,
            "max_tokens": max_tokens,
            "system": system_prompt,
            "messages": [{"role": "user", "content": user_prompt}],
        }
        start = time.perf_counter()
        try:
            # with_raw_response gives access to the literal httpx request/
            # response the SDK sent, not just the parsed Message object.
            raw = self._client.messages.with_raw_response.create(
                model=self._model,
                max_tokens=max_tokens,
                system=system_prompt,
                messages=[{"role": "user", "content": user_prompt}],
            )
            latency_ms = int((time.perf_counter() - start) * 1000)
            message = raw.parse()

            http_response = raw.http_response
            http_request = http_response.request

            http_trace = HttpTrace(
                http_method=http_request.method,
                request_url=str(http_request.url),
                request_headers=_redact_headers(dict(http_request.headers)),
                request_body=_safe_json(http_request.content),
                response_status_code=http_response.status_code,
                response_headers=dict(http_response.headers),
                response_body=_safe_json(http_response.content),
            )

            text = "".join(block.text for block in message.content if block.type == "text")
            prompt_tokens = message.usage.input_tokens
            completion_tokens = message.usage.output_tokens
            return LlmCallResult(
                text=text,
                prompt_tokens=prompt_tokens,
                completion_tokens=completion_tokens,
                estimated_cost_usd=_estimate_cost(self._model, prompt_tokens, completion_tokens),
                latency_ms=latency_ms,
                request_payload=request_payload,
                response_payload={"content": [{"type": "text", "text": text}]},
                success=True,
                http_trace=http_trace,
            )
        except Exception as exc:  # noqa: BLE001 — surfaced via the audit trail, not swallowed
            latency_ms = int((time.perf_counter() - start) * 1000)
            logger.exception("Anthropic call failed")
            return LlmCallResult(
                text="",
                prompt_tokens=None,
                completion_tokens=None,
                estimated_cost_usd=None,
                latency_ms=latency_ms,
                request_payload=request_payload,
                response_payload=None,
                success=False,
                error_message=str(exc),
                http_trace=None,
            )


def parse_json_response(text: str, fallback: dict) -> dict:
    """LLM output is instructed to be strict JSON, but never trust that
    blindly — fall back to a safe, explicit default and keep the raw text
    for the audit trail rather than crashing the run on one bad response.
    """
    try:
        # Models occasionally wrap JSON in a ```json fence despite instructions.
        cleaned = text.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        return json.loads(cleaned)
    except (json.JSONDecodeError, AttributeError):
        logger.warning("Could not parse LLM JSON response, using fallback. Raw text: %r", text)
        return {**fallback, "_raw_text": text, "_parse_error": True}
