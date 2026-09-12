-- ============================================================================
-- V3 — Adds llm_call_http_trace: raw wire-level HTTP detail for each LLM
-- call, separate from llm_call_log's logical request/response payloads.
--
-- llm_call_log.request_payload / response_payload are a reconstruction of
-- the *logical* message content (model, prompt, parsed reply) — useful for
-- the cost/decision dashboard, but not the literal bytes sent over the
-- wire. This table captures the actual URL, HTTP method, headers, and raw
-- body for both the request and response, one row per llm_call_log row.
--
-- SECURITY: request_headers NEVER contains a real API key. The Python
-- agent replaces the Authorization / x-api-key header value with the
-- literal string "REDACTED" before this row is ever created — enforced in
-- application code (llm_clients.py), not just by convention. Never relax
-- this to store a real credential, even in a local demo database.
-- ============================================================================

USE retail_replenishment;

CREATE TABLE llm_call_http_trace (
                                     http_trace_id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
                                     llm_call_id             BIGINT UNSIGNED  NOT NULL,
                                     http_method               VARCHAR(10)   NOT NULL DEFAULT 'POST',
                                     request_url                 VARCHAR(500) NOT NULL,
                                     request_headers                JSON      NOT NULL,
                                     request_params                   JSON    NULL,
                                     request_body                       JSON  NOT NULL,
                                     response_status_code                  SMALLINT UNSIGNED NULL,
                                     response_headers                         JSON NULL,
                                     response_body                             JSON NULL,
                                     created_at                                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                     CONSTRAINT pk_llm_call_http_trace PRIMARY KEY (http_trace_id),
                                     CONSTRAINT uq_llm_call_http_trace_call UNIQUE (llm_call_id),
                                     CONSTRAINT fk_http_trace_llm_call FOREIGN KEY (llm_call_id) REFERENCES llm_call_log (llm_call_id)
                                         ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_http_trace_llm_call ON llm_call_http_trace (llm_call_id);