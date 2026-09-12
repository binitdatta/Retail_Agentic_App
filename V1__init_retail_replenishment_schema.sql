-- ============================================================================
-- Retail Replenishment Agentic POC — Schema DDL
-- Target: MySQL 8.0.16+ (CHECK constraints enforced), InnoDB, utf8mb4
-- Ownership: DBA-owned. Spring Boot uses ddl-auto: validate against this file.
-- Migration tool: Flyway-style versioned script (V1__init_retail_replenishment_schema.sql)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS retail_replenishment
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE retail_replenishment;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- SECTION 1: REFERENCE / LOOKUP TABLES
-- Normalized status domains instead of MySQL ENUM columns, so new statuses
-- can be added without an ALTER TABLE, and each status carries metadata.
-- ============================================================================

CREATE TABLE ref_order_status (
    status_code   VARCHAR(30)   NOT NULL,
    description   VARCHAR(150)  NOT NULL,
    is_terminal   TINYINT(1)    NOT NULL DEFAULT 0,
    sort_order    INT UNSIGNED  NOT NULL,
    CONSTRAINT pk_ref_order_status PRIMARY KEY (status_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO ref_order_status (status_code, description, is_terminal, sort_order) VALUES
    ('DRAFT',               'Draft, not yet recommended',            0, 10),
    ('RECOMMENDED',         'Agent-recommended, awaiting review',     0, 20),
    ('PENDING_APPROVAL',    'Submitted for human approval',           0, 30),
    ('APPROVED',            'Approved, ready to send to supplier',    0, 40),
    ('SENT_TO_SUPPLIER',    'Transmitted to supplier',                0, 50),
    ('IN_TRANSIT',          'Shipment in transit',                    0, 60),
    ('DELIVERED',           'Delivered and received into inventory',  1, 70),
    ('CANCELLED',           'Order cancelled',                        1, 80),
    ('SHORTAGE_ESCALATED',  'Could not be fulfilled, escalated',      1, 90);

CREATE TABLE ref_shipment_status (
    status_code   VARCHAR(30)   NOT NULL,
    description   VARCHAR(150)  NOT NULL,
    is_terminal   TINYINT(1)    NOT NULL DEFAULT 0,
    sort_order    INT UNSIGNED  NOT NULL,
    CONSTRAINT pk_ref_shipment_status PRIMARY KEY (status_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO ref_shipment_status (status_code, description, is_terminal, sort_order) VALUES
    ('CREATED',          'Shipment record created',       0, 10),
    ('DEPARTED',         'Departed origin facility',       0, 20),
    ('IN_TRANSIT',       'In transit',                     0, 30),
    ('OUT_FOR_DELIVERY', 'Out for delivery',                0, 40),
    ('DELIVERED',        'Delivered',                       1, 50),
    ('DELAYED',          'Delayed beyond ETA',              0, 60),
    ('EXCEPTION',        'Delivery exception / lost',       1, 70);

CREATE TABLE ref_escalation_status (
    status_code   VARCHAR(30)   NOT NULL,
    description   VARCHAR(150)  NOT NULL,
    is_terminal   TINYINT(1)    NOT NULL DEFAULT 0,
    sort_order    INT UNSIGNED  NOT NULL,
    CONSTRAINT pk_ref_escalation_status PRIMARY KEY (status_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO ref_escalation_status (status_code, description, is_terminal, sort_order) VALUES
    ('OPEN',          'Newly raised, unassigned',   0, 10),
    ('ACKNOWLEDGED',  'Acknowledged by a human',    0, 20),
    ('IN_PROGRESS',   'Being worked',                0, 30),
    ('RESOLVED',      'Resolved',                    1, 40),
    ('CLOSED',        'Closed without resolution',   1, 50);

CREATE TABLE ref_sku_category (
    category_code VARCHAR(40)   NOT NULL,
    description   VARCHAR(150)  NOT NULL,
    CONSTRAINT pk_ref_sku_category PRIMARY KEY (category_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE ref_uom (
    uom_code      VARCHAR(10)   NOT NULL,
    description   VARCHAR(80)   NOT NULL,
    CONSTRAINT pk_ref_uom PRIMARY KEY (uom_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO ref_uom (uom_code, description) VALUES
    ('EACH', 'Each / unit'),
    ('CASE', 'Case pack'),
    ('BOX',  'Box'),
    ('KG',   'Kilogram'),
    ('LB',   'Pound');

CREATE TABLE ref_llm_provider (
    provider_code VARCHAR(30)   NOT NULL,
    description   VARCHAR(150)  NOT NULL,
    is_active     TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT pk_ref_llm_provider PRIMARY KEY (provider_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO ref_llm_provider (provider_code, description) VALUES
    ('ANTHROPIC',        'Anthropic Claude API'),
    ('AZURE_AI_FOUNDRY',  'Azure AI Foundry (e.g. Llama-3.3-70B)');

-- ============================================================================
-- SECTION 2: MASTER / DIMENSION TABLES
-- ============================================================================

CREATE TABLE store (
    store_id       BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    store_code     VARCHAR(20)      NOT NULL,
    store_name     VARCHAR(120)     NOT NULL,
    region         VARCHAR(60)      NULL,
    address_line1  VARCHAR(150)     NULL,
    city           VARCHAR(100)     NULL,
    state_province VARCHAR(100)     NULL,
    postal_code    VARCHAR(20)      NULL,
    country_code   CHAR(2)          NOT NULL DEFAULT 'US',
    timezone       VARCHAR(50)      NOT NULL DEFAULT 'UTC',
    is_active      TINYINT(1)       NOT NULL DEFAULT 1,
    created_at     DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by     VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    updated_at     DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by     VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    version        BIGINT           NOT NULL DEFAULT 0,
    CONSTRAINT pk_store PRIMARY KEY (store_id),
    CONSTRAINT uq_store_code UNIQUE (store_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE product (
    product_id       BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    sku_code         VARCHAR(40)      NOT NULL,
    product_name     VARCHAR(200)     NOT NULL,
    category_code    VARCHAR(40)      NULL,
    uom_code         VARCHAR(10)      NOT NULL DEFAULT 'EACH',
    unit_cost        DECIMAL(12,4)    NOT NULL DEFAULT 0.0000,
    unit_price       DECIMAL(12,4)    NOT NULL DEFAULT 0.0000,
    is_seasonal      TINYINT(1)       NOT NULL DEFAULT 0,
    is_critical      TINYINT(1)       NOT NULL DEFAULT 0,
    shelf_life_days  INT UNSIGNED     NULL,
    is_active        TINYINT(1)       NOT NULL DEFAULT 1,
    created_at       DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    updated_at       DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by       VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    version          BIGINT           NOT NULL DEFAULT 0,
    CONSTRAINT pk_product PRIMARY KEY (product_id),
    CONSTRAINT uq_product_sku_code UNIQUE (sku_code),
    CONSTRAINT fk_product_category FOREIGN KEY (category_code) REFERENCES ref_sku_category (category_code)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_product_uom FOREIGN KEY (uom_code) REFERENCES ref_uom (uom_code)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_product_costs_nonneg CHECK (unit_cost >= 0 AND unit_price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE supplier (
    supplier_id         BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    supplier_code       VARCHAR(30)      NOT NULL,
    legal_name          VARCHAR(200)     NOT NULL,
    contact_email       VARCHAR(150)     NULL,
    contact_phone       VARCHAR(30)      NULL,
    avg_lead_time_days  INT UNSIGNED     NOT NULL DEFAULT 0,
    reliability_score   DECIMAL(5,4)     NOT NULL DEFAULT 1.0000,
    is_active           TINYINT(1)       NOT NULL DEFAULT 1,
    created_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by          VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    updated_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by          VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    version              BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplier PRIMARY KEY (supplier_id),
    CONSTRAINT uq_supplier_code UNIQUE (supplier_code),
    CONSTRAINT chk_supplier_reliability CHECK (reliability_score >= 0 AND reliability_score <= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE supplier_product (
    supplier_product_id  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    supplier_id          BIGINT UNSIGNED  NOT NULL,
    product_id           BIGINT UNSIGNED  NOT NULL,
    supplier_sku         VARCHAR(60)      NULL,
    unit_cost            DECIMAL(12,4)    NOT NULL,
    min_order_qty        DECIMAL(14,4)    NOT NULL DEFAULT 1.0000,
    lead_time_days       INT UNSIGNED     NOT NULL,
    is_preferred         TINYINT(1)       NOT NULL DEFAULT 0,
    created_at           DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by           VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    updated_at           DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by           VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT pk_supplier_product PRIMARY KEY (supplier_product_id),
    CONSTRAINT uq_supplier_product UNIQUE (supplier_id, product_id),
    CONSTRAINT fk_supplier_product_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_supplier_product_product FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_supplier_product_moq CHECK (min_order_qty > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================================
-- SECTION 3: INVENTORY & DEMAND
-- ============================================================================

CREATE TABLE store_inventory (
    store_inventory_id  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    store_id            BIGINT UNSIGNED  NOT NULL,
    product_id          BIGINT UNSIGNED  NOT NULL,
    on_hand_qty         DECIMAL(14,4)    NOT NULL DEFAULT 0.0000,
    allocated_qty       DECIMAL(14,4)    NOT NULL DEFAULT 0.0000,
    reorder_point       DECIMAL(14,4)    NOT NULL,
    reorder_qty         DECIMAL(14,4)    NOT NULL,
    safety_stock_qty    DECIMAL(14,4)    NOT NULL DEFAULT 0.0000,
    max_stock_qty       DECIMAL(14,4)    NULL,
    last_counted_at     DATETIME(6)      NULL,
    created_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by          VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    updated_at          DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by           VARCHAR(100)    NOT NULL DEFAULT 'SYSTEM',
    version              BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_store_inventory PRIMARY KEY (store_inventory_id),
    CONSTRAINT uq_store_inventory UNIQUE (store_id, product_id),
    CONSTRAINT fk_store_inventory_store FOREIGN KEY (store_id) REFERENCES store (store_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_store_inventory_product FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_store_inventory_qty_nonneg CHECK (on_hand_qty >= 0 AND allocated_qty >= 0),
    CONSTRAINT chk_store_inventory_reorder_nonneg CHECK (reorder_point >= 0 AND reorder_qty > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_store_inventory_product ON store_inventory (product_id);

CREATE TABLE sales_transaction_daily (
    daily_sales_id  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    store_id        BIGINT UNSIGNED  NOT NULL,
    product_id      BIGINT UNSIGNED  NOT NULL,
    sales_date      DATE             NOT NULL,
    units_sold      DECIMAL(14,4)    NOT NULL DEFAULT 0.0000,
    gross_revenue   DECIMAL(14,4)    NOT NULL DEFAULT 0.0000,
    created_at      DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_sales_transaction_daily PRIMARY KEY (daily_sales_id),
    CONSTRAINT uq_sales_transaction_daily UNIQUE (store_id, product_id, sales_date),
    CONSTRAINT fk_sales_daily_store FOREIGN KEY (store_id) REFERENCES store (store_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_sales_daily_product FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_sales_daily_nonneg CHECK (units_sold >= 0 AND gross_revenue >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_sales_daily_product_date ON sales_transaction_daily (product_id, sales_date);

-- ============================================================================
-- SECTION 4: AGENT RUN / DECISION AUDIT
-- Defined before replenishment_order / demand_forecast_run since both
-- reference agent_run_id for traceability.
-- ============================================================================

CREATE TABLE agent_run (
    agent_run_id    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    run_uuid        CHAR(36)         NOT NULL,
    trigger_type    VARCHAR(20)      NOT NULL,
    trigger_source  VARCHAR(100)     NULL,
    store_id        BIGINT UNSIGNED  NULL,
    started_at      DATETIME(6)      NOT NULL,
    completed_at    DATETIME(6)      NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'RUNNING',
    summary         VARCHAR(2000)    NULL,
    CONSTRAINT pk_agent_run PRIMARY KEY (agent_run_id),
    CONSTRAINT uq_agent_run_uuid UNIQUE (run_uuid),
    CONSTRAINT fk_agent_run_store FOREIGN KEY (store_id) REFERENCES store (store_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_agent_run_trigger_type CHECK (trigger_type IN ('SCHEDULED', 'EVENT', 'MANUAL')),
    CONSTRAINT chk_agent_run_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'TIMED_OUT'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_agent_run_started_at ON agent_run (started_at);

CREATE TABLE agent_decision_log (
    decision_id      BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    agent_run_id     BIGINT UNSIGNED  NOT NULL,
    stage_name       VARCHAR(60)      NOT NULL,
    store_id         BIGINT UNSIGNED  NULL,
    product_id       BIGINT UNSIGNED  NULL,
    input_snapshot   JSON             NOT NULL,
    output_decision  JSON             NOT NULL,
    llm_model        VARCHAR(60)      NULL,
    llm_rationale    TEXT             NULL,
    executed_at      DATETIME(6)      NOT NULL,
    duration_ms      INT UNSIGNED     NULL,
    CONSTRAINT pk_agent_decision_log PRIMARY KEY (decision_id),
    CONSTRAINT fk_decision_log_run FOREIGN KEY (agent_run_id) REFERENCES agent_run (agent_run_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_decision_log_store FOREIGN KEY (store_id) REFERENCES store (store_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_decision_log_product FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_decision_log_stage CHECK (stage_name IN (
        'DETECT_LOW_INVENTORY', 'FORECAST_DEMAND', 'CHECK_SUPPLIER_AVAILABILITY',
        'RECOMMEND_REPLENISHMENT', 'MONITOR_DELIVERY', 'ESCALATE_SHORTAGE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_decision_log_run_time ON agent_decision_log (agent_run_id, executed_at);

CREATE TABLE demand_forecast_run (
    forecast_run_id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    store_id                  BIGINT UNSIGNED  NOT NULL,
    product_id                BIGINT UNSIGNED  NOT NULL,
    forecast_method           VARCHAR(40)      NOT NULL,
    forecast_horizon_days     INT UNSIGNED     NOT NULL,
    forecasted_demand_qty     DECIMAL(14,4)    NOT NULL,
    confidence_score          DECIMAL(5,4)     NULL,
    generated_at              DATETIME(6)      NOT NULL,
    generated_by_agent_run_id BIGINT UNSIGNED  NULL,
    CONSTRAINT pk_demand_forecast_run PRIMARY KEY (forecast_run_id),
    CONSTRAINT fk_forecast_store FOREIGN KEY (store_id) REFERENCES store (store_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_forecast_product FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_forecast_agent_run FOREIGN KEY (generated_by_agent_run_id) REFERENCES agent_run (agent_run_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_forecast_qty_nonneg CHECK (forecasted_demand_qty >= 0),
    CONSTRAINT chk_forecast_confidence CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_forecast_store_product_time ON demand_forecast_run (store_id, product_id, generated_at);

-- ============================================================================
-- SECTION 5: REPLENISHMENT ORDERS
-- ============================================================================

CREATE TABLE replenishment_order (
    replenishment_order_id    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    order_number               VARCHAR(40)      NOT NULL,
    store_id                   BIGINT UNSIGNED  NOT NULL,
    supplier_id                BIGINT UNSIGNED  NOT NULL,
    status_code                VARCHAR(30)      NOT NULL DEFAULT 'DRAFT',
    source_type                VARCHAR(20)      NOT NULL,
    generated_by_agent_run_id  BIGINT UNSIGNED  NULL,
    total_cost                 DECIMAL(14,4)    NOT NULL DEFAULT 0.0000,
    requested_delivery_date    DATE             NULL,
    approved_by                VARCHAR(100)     NULL,
    approved_at                DATETIME(6)      NULL,
    sent_to_supplier_at        DATETIME(6)      NULL,
    created_at                 DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by                 VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    updated_at                 DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by                 VARCHAR(100)     NOT NULL DEFAULT 'SYSTEM',
    version                    BIGINT           NOT NULL DEFAULT 0,
    CONSTRAINT pk_replenishment_order PRIMARY KEY (replenishment_order_id),
    CONSTRAINT uq_replenishment_order_number UNIQUE (order_number),
    CONSTRAINT fk_repl_order_store FOREIGN KEY (store_id) REFERENCES store (store_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_repl_order_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (supplier_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_repl_order_status FOREIGN KEY (status_code) REFERENCES ref_order_status (status_code)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_repl_order_agent_run FOREIGN KEY (generated_by_agent_run_id) REFERENCES agent_run (agent_run_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_repl_order_source CHECK (source_type IN ('AGENT', 'MANUAL', 'SCHEDULED')),
    CONSTRAINT chk_repl_order_total_nonneg CHECK (total_cost >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_repl_order_status ON replenishment_order (status_code);
CREATE INDEX idx_repl_order_store_status ON replenishment_order (store_id, status_code);

CREATE TABLE replenishment_order_line (
    order_line_id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    replenishment_order_id   BIGINT UNSIGNED  NOT NULL,
    product_id                BIGINT UNSIGNED  NOT NULL,
    ordered_qty                DECIMAL(14,4)   NOT NULL,
    unit_cost                  DECIMAL(12,4)   NOT NULL,
    line_total                 DECIMAL(18,4)   GENERATED ALWAYS AS (ordered_qty * unit_cost) STORED,
    forecast_run_id            BIGINT UNSIGNED  NULL,
    CONSTRAINT pk_repl_order_line PRIMARY KEY (order_line_id),
    CONSTRAINT uq_repl_order_line UNIQUE (replenishment_order_id, product_id),
    CONSTRAINT fk_repl_order_line_order FOREIGN KEY (replenishment_order_id)
        REFERENCES replenishment_order (replenishment_order_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_repl_order_line_product FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_repl_order_line_forecast FOREIGN KEY (forecast_run_id)
        REFERENCES demand_forecast_run (forecast_run_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_repl_order_line_qty_pos CHECK (ordered_qty > 0),
    CONSTRAINT chk_repl_order_line_cost_nonneg CHECK (unit_cost >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================================
-- SECTION 6: DELIVERY / SHIPMENT TRACKING
-- ============================================================================

CREATE TABLE delivery_shipment (
    shipment_id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    replenishment_order_id   BIGINT UNSIGNED  NOT NULL,
    carrier_name              VARCHAR(100)    NULL,
    tracking_number            VARCHAR(80)    NULL,
    status_code                 VARCHAR(30)   NOT NULL DEFAULT 'CREATED',
    shipped_at                  DATETIME(6)   NULL,
    estimated_arrival_at        DATETIME(6)   NULL,
    actual_arrival_at           DATETIME(6)   NULL,
    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_delivery_shipment PRIMARY KEY (shipment_id),
    CONSTRAINT fk_shipment_order FOREIGN KEY (replenishment_order_id)
        REFERENCES replenishment_order (replenishment_order_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_shipment_status FOREIGN KEY (status_code) REFERENCES ref_shipment_status (status_code)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_shipment_order ON delivery_shipment (replenishment_order_id);

CREATE TABLE delivery_tracking_event (
    tracking_event_id  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    shipment_id         BIGINT UNSIGNED  NOT NULL,
    event_code            VARCHAR(40)   NOT NULL,
    event_at               DATETIME(6)  NOT NULL,
    event_location          VARCHAR(150) NULL,
    notes                    VARCHAR(500) NULL,
    CONSTRAINT pk_delivery_tracking_event PRIMARY KEY (tracking_event_id),
    CONSTRAINT fk_tracking_event_shipment FOREIGN KEY (shipment_id) REFERENCES delivery_shipment (shipment_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_tracking_event_code CHECK (event_code IN (
        'DEPARTED', 'IN_TRANSIT', 'CUSTOMS', 'OUT_FOR_DELIVERY', 'DELIVERED', 'DELAYED', 'EXCEPTION'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_tracking_event_shipment_time ON delivery_tracking_event (shipment_id, event_at);

-- ============================================================================
-- SECTION 7: SHORTAGE ESCALATION
-- ============================================================================

CREATE TABLE shortage_escalation (
    escalation_id            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    replenishment_order_id   BIGINT UNSIGNED  NULL,
    store_id                  BIGINT UNSIGNED  NOT NULL,
    product_id                 BIGINT UNSIGNED  NOT NULL,
    escalation_reason           VARCHAR(60)   NOT NULL,
    severity                     VARCHAR(20)  NOT NULL,
    status_code                   VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    raised_by_agent_run_id        BIGINT UNSIGNED  NULL,
    assigned_to                    VARCHAR(100) NULL,
    resolved_at                     DATETIME(6) NULL,
    resolution_notes                 VARCHAR(1000) NULL,
    created_at                        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_shortage_escalation PRIMARY KEY (escalation_id),
    CONSTRAINT fk_escalation_order FOREIGN KEY (replenishment_order_id)
        REFERENCES replenishment_order (replenishment_order_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_escalation_store FOREIGN KEY (store_id) REFERENCES store (store_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_escalation_product FOREIGN KEY (product_id) REFERENCES product (product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_escalation_status FOREIGN KEY (status_code) REFERENCES ref_escalation_status (status_code)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_escalation_agent_run FOREIGN KEY (raised_by_agent_run_id) REFERENCES agent_run (agent_run_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_escalation_reason CHECK (escalation_reason IN (
        'SUPPLIER_UNAVAILABLE', 'DELIVERY_DELAYED', 'DEMAND_SPIKE', 'CAPACITY_CONSTRAINT', 'OTHER')),
    CONSTRAINT chk_escalation_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_escalation_status ON shortage_escalation (status_code);
CREATE INDEX idx_escalation_store_product ON shortage_escalation (store_id, product_id);

-- ============================================================================
-- SECTION 8: LLM CALL AUDIT
-- Same pattern as the SpecRight SDM chatbot's cost/audit dashboard table:
-- one row per LLM invocation, full request/response payloads for replay and
-- debugging, token/cost accounting for dashboard rollups. Linked down to the
-- specific agent_decision_log row so a dashboard can answer "which LLM call
-- produced this decision" as well as "what did this run cost."
-- ============================================================================

CREATE TABLE llm_call_log (
    llm_call_id        BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    agent_run_id        BIGINT UNSIGNED  NOT NULL,
    decision_id          BIGINT UNSIGNED  NULL,
    provider_code          VARCHAR(30)   NOT NULL,
    model_name               VARCHAR(80) NOT NULL,
    request_payload            JSON      NOT NULL,
    response_payload            JSON     NULL,
    prompt_tokens                 INT UNSIGNED NULL,
    completion_tokens               INT UNSIGNED NULL,
    total_tokens                       INT UNSIGNED GENERATED ALWAYS AS
        (COALESCE(prompt_tokens, 0) + COALESCE(completion_tokens, 0)) STORED,
    estimated_cost_usd                    DECIMAL(12,6) NULL,
    latency_ms                               INT UNSIGNED NULL,
    http_status_code                            SMALLINT UNSIGNED NULL,
    is_success                                     TINYINT(1) NOT NULL DEFAULT 1,
    error_message                                     VARCHAR(1000) NULL,
    requested_at                                         DATETIME(6) NOT NULL,
    completed_at                                            DATETIME(6) NULL,
    CONSTRAINT pk_llm_call_log PRIMARY KEY (llm_call_id),
    CONSTRAINT fk_llm_call_agent_run FOREIGN KEY (agent_run_id) REFERENCES agent_run (agent_run_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_llm_call_decision FOREIGN KEY (decision_id) REFERENCES agent_decision_log (decision_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_llm_call_provider FOREIGN KEY (provider_code) REFERENCES ref_llm_provider (provider_code)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_llm_call_cost_nonneg CHECK (estimated_cost_usd IS NULL OR estimated_cost_usd >= 0),
    CONSTRAINT chk_llm_call_tokens_nonneg CHECK (
        (prompt_tokens IS NULL OR prompt_tokens >= 0) AND
        (completion_tokens IS NULL OR completion_tokens >= 0))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Dashboard access patterns: cost/volume by run, by provider+model, and a
-- success/error filter for reliability charts.
CREATE INDEX idx_llm_call_run_time ON llm_call_log (agent_run_id, requested_at);
CREATE INDEX idx_llm_call_provider_model ON llm_call_log (provider_code, model_name);
CREATE INDEX idx_llm_call_success ON llm_call_log (is_success);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- SECTION 9: CONVENIENCE VIEW — low-stock detection query the agent's first
-- stage runs every cycle. Kept as a view so the "detect" query stays a single
-- reviewable artifact instead of duplicated logic in application code.
-- ============================================================================

CREATE OR REPLACE VIEW vw_low_stock_inventory AS
SELECT
    si.store_inventory_id,
    si.store_id,
    s.store_code,
    si.product_id,
    p.sku_code,
    p.product_name,
    p.is_critical,
    si.on_hand_qty,
    si.allocated_qty,
    (si.on_hand_qty - si.allocated_qty) AS available_qty,
    si.reorder_point,
    si.reorder_qty,
    si.safety_stock_qty
FROM store_inventory si
JOIN store   s ON s.store_id = si.store_id
JOIN product p ON p.product_id = si.product_id
WHERE (si.on_hand_qty - si.allocated_qty) <= si.reorder_point
  AND s.is_active = 1
  AND p.is_active = 1;

CREATE OR REPLACE VIEW vw_llm_cost_by_run AS
SELECT
    ar.agent_run_id,
    ar.run_uuid,
    ar.started_at,
    ar.completed_at,
    ar.status AS run_status,
    COUNT(lcl.llm_call_id)              AS llm_call_count,
    SUM(lcl.prompt_tokens)               AS total_prompt_tokens,
    SUM(lcl.completion_tokens)            AS total_completion_tokens,
    SUM(lcl.total_tokens)                  AS total_tokens,
    SUM(lcl.estimated_cost_usd)             AS total_estimated_cost_usd,
    AVG(lcl.latency_ms)                      AS avg_latency_ms,
    SUM(CASE WHEN lcl.is_success = 0 THEN 1 ELSE 0 END) AS error_count
FROM agent_run ar
LEFT JOIN llm_call_log lcl ON lcl.agent_run_id = ar.agent_run_id
GROUP BY ar.agent_run_id, ar.run_uuid, ar.started_at, ar.completed_at, ar.status;
