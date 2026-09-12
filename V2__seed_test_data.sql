-- ============================================================================
-- Retail Replenishment Agentic POC — Seed / Test Data
-- Depends on: V1__init_retail_replenishment_schema.sql
-- Target: MySQL 8.0.14+ (uses recursive CTE + LATERAL derived tables)
--
-- Deliberately builds in TWO demo scenarios so a single agent run exercises
-- both branches of the decision graph:
--   1. HAPPY PATH   — Whole Milk @ Downtown Flagship: low stock -> forecast
--                      -> supplier available -> order recommended -> shipped.
--   2. ESCALATION    — Sourdough @ Downtown Flagship: low stock -> forecast
--                      -> NO supplier mapped for BAKERY -> shortage escalated.
-- Plus three more low-stock triggers (seasonal spike, boundary-condition,
-- household) purely for the detection-stage view, left unresolved so you
-- have open work items to show in a dashboard.
-- ============================================================================

USE retail_replenishment;

-- ============================================================================
-- SECTION 1: REFERENCE DATA — SKU categories
-- (Statuses / UOM / LLM providers already seeded by V1.)
-- ============================================================================

INSERT INTO ref_sku_category (category_code, description) VALUES
    ('BEVERAGES', 'Beverages'),
    ('SNACKS',    'Snacks'),
    ('DAIRY',     'Dairy'),
    ('BAKERY',    'Bakery'),
    ('FROZEN',    'Frozen'),
    ('HOUSEHOLD', 'Household goods');

-- ============================================================================
-- SECTION 2: STORES
-- ============================================================================

INSERT INTO store (store_code, store_name, region, address_line1, city, state_province, postal_code, country_code, timezone) VALUES
    ('ST-100', 'Downtown Flagship',  'Midwest', '100 Michigan Ave',  'Chicago', 'IL', '60601', 'US', 'America/Chicago'),
    ('ST-101', 'Northside Express',  'Midwest', '4500 Oakton St',    'Skokie',  'IL', '60076', 'US', 'America/Chicago'),
    ('ST-200', 'Riverside Market',   'West',    '2200 Larimer St',   'Denver',  'CO', '80205', 'US', 'America/Denver');

SET @st_100 = (SELECT store_id FROM store WHERE store_code = 'ST-100');
SET @st_101 = (SELECT store_id FROM store WHERE store_code = 'ST-101');
SET @st_200 = (SELECT store_id FROM store WHERE store_code = 'ST-200');

-- ============================================================================
-- SECTION 3: PRODUCTS
-- ============================================================================

INSERT INTO product (sku_code, product_name, category_code, uom_code, unit_cost, unit_price, is_seasonal, is_critical, shelf_life_days) VALUES
    ('SKU-BEV-001', 'Sparkling Water 12-Pack',        'BEVERAGES', 'EACH', 4.5000, 7.9900, 0, 0, NULL),
    ('SKU-BEV-002', 'Cold Brew Coffee 32oz',           'BEVERAGES', 'EACH', 3.2000, 5.9900, 0, 0, NULL),
    ('SKU-SNK-001', 'Kettle Chips Sea Salt',            'SNACKS',    'EACH', 1.8000, 3.4900, 0, 0, NULL),
    ('SKU-SNK-002', 'Trail Mix Family Pack',             'SNACKS',    'EACH', 4.1000, 7.4900, 0, 0, NULL),
    ('SKU-DRY-001', 'Whole Milk Gallon',                  'DAIRY',     'EACH', 2.4000, 3.9900, 0, 1, 10),
    ('SKU-DRY-002', 'Greek Yogurt 32oz Tub',               'DAIRY',     'EACH', 3.6000, 5.4900, 0, 0, 21),
    ('SKU-BAK-001', 'Artisan Sourdough Loaf',               'BAKERY',    'EACH', 1.9000, 4.9900, 0, 0, 3),
    ('SKU-FRZ-001', 'Frozen Pizza Margherita',               'FROZEN',    'EACH', 3.1000, 6.9900, 0, 0, NULL),
    ('SKU-FRZ-002', 'Ice Cream Vanilla Quart',                 'FROZEN',    'EACH', 2.7500, 5.4900, 1, 0, NULL),
    ('SKU-HH-001',  'Paper Towels 6-Pack',                      'HOUSEHOLD', 'EACH', 5.2000, 9.9900, 0, 0, NULL);

SET @p_bev1  = (SELECT product_id FROM product WHERE sku_code = 'SKU-BEV-001');
SET @p_bev2  = (SELECT product_id FROM product WHERE sku_code = 'SKU-BEV-002');
SET @p_snk1  = (SELECT product_id FROM product WHERE sku_code = 'SKU-SNK-001');
SET @p_snk2  = (SELECT product_id FROM product WHERE sku_code = 'SKU-SNK-002');
SET @p_milk  = (SELECT product_id FROM product WHERE sku_code = 'SKU-DRY-001');
SET @p_yog   = (SELECT product_id FROM product WHERE sku_code = 'SKU-DRY-002');
SET @p_bread = (SELECT product_id FROM product WHERE sku_code = 'SKU-BAK-001');
SET @p_pizza = (SELECT product_id FROM product WHERE sku_code = 'SKU-FRZ-001');
SET @p_icecream = (SELECT product_id FROM product WHERE sku_code = 'SKU-FRZ-002');
SET @p_towels = (SELECT product_id FROM product WHERE sku_code = 'SKU-HH-001');

-- ============================================================================
-- SECTION 4: SUPPLIERS + SUPPLIER-PRODUCT MAPPINGS
-- NOTE: BAKERY (SKU-BAK-001) is deliberately left with no supplier mapping —
-- this is what drives the SUPPLIER_UNAVAILABLE escalation scenario below.
-- ============================================================================

INSERT INTO supplier (supplier_code, legal_name, contact_email, contact_phone, avg_lead_time_days, reliability_score) VALUES
    ('SUP-001', 'Coastal Beverage Distributors',        'orders@coastalbev.example.com',   '312-555-0101', 3, 0.9800),
    ('SUP-002', 'Heartland Grocery Supply',               'orders@heartlandgrocery.example.com', '312-555-0102', 2, 0.9500),
    ('SUP-003', 'FreshChain Dairy & Frozen Logistics',      'orders@freshchainlogistics.example.com', '312-555-0103', 1, 0.9000);

SET @sup_bev    = (SELECT supplier_id FROM supplier WHERE supplier_code = 'SUP-001');
SET @sup_grocery = (SELECT supplier_id FROM supplier WHERE supplier_code = 'SUP-002');
SET @sup_dairy   = (SELECT supplier_id FROM supplier WHERE supplier_code = 'SUP-003');

INSERT INTO supplier_product (supplier_id, product_id, supplier_sku, unit_cost, min_order_qty, lead_time_days, is_preferred) VALUES
    (@sup_bev,     @p_bev1,     'CBD-SPW-12',  4.1000, 24, 3, 1),
    (@sup_bev,     @p_bev2,     'CBD-CBC-32',  2.9000, 12, 3, 1),
    (@sup_grocery, @p_snk1,     'HGS-KCH-SS',  1.6000, 24, 2, 1),
    (@sup_grocery, @p_snk2,     'HGS-TMX-FP',  3.7000, 12, 2, 1),
    (@sup_grocery, @p_towels,   'HGS-PTW-6PK', 4.8000, 12, 2, 1),
    (@sup_dairy,   @p_milk,     'FCL-MLK-GAL', 2.3000, 24, 1, 1),
    (@sup_dairy,   @p_yog,      'FCL-YOG-32',  3.3000, 12, 1, 1),
    (@sup_dairy,   @p_pizza,    'FCL-PIZ-MRG', 2.9000, 12, 1, 1),
    (@sup_dairy,   @p_icecream, 'FCL-ICE-VAN', 2.5000, 12, 1, 1);

-- ============================================================================
-- SECTION 5: STORE INVENTORY
-- Five rows are deliberately below (on_hand - allocated) <= reorder_point so
-- vw_low_stock_inventory returns a realistic, varied worklist:
--   ST-100 / Whole Milk        -> critical SKU, supplier available
--   ST-100 / Sourdough         -> no supplier mapped -> escalation scenario
--   ST-101 / Ice Cream Vanilla -> seasonal demand spike
--   ST-200 / Kettle Chips      -> boundary condition (available == reorder_point)
--   ST-200 / Paper Towels      -> household, plain replenishment
-- ============================================================================

INSERT INTO store_inventory (store_id, product_id, on_hand_qty, allocated_qty, reorder_point, reorder_qty, safety_stock_qty, max_stock_qty, last_counted_at) VALUES
    -- ST-100 Downtown Flagship
    (@st_100, @p_bev1,     95, 5, 20, 60, 10, 200, NOW(6) - INTERVAL 2 DAY),
    (@st_100, @p_bev2,     60, 3, 15, 40,  8, 150, NOW(6) - INTERVAL 2 DAY),
    (@st_100, @p_snk1,    110, 8, 25, 75, 10, 250, NOW(6) - INTERVAL 2 DAY),
    (@st_100, @p_snk2,     70, 4, 18, 50,  8, 180, NOW(6) - INTERVAL 2 DAY),
    (@st_100, @p_milk,      8, 1, 20, 80, 15, 200, NOW(6) - INTERVAL 1 DAY),   -- LOW STOCK, critical
    (@st_100, @p_yog,      55, 2, 15, 45,  8, 150, NOW(6) - INTERVAL 1 DAY),
    (@st_100, @p_bread,     5, 0, 15, 40,  5, 120, NOW(6) - INTERVAL 1 DAY),   -- LOW STOCK, no supplier
    (@st_100, @p_pizza,    48, 3, 12, 36,  6, 120, NOW(6) - INTERVAL 2 DAY),
    (@st_100, @p_icecream, 90, 4, 25, 70, 10, 220, NOW(6) - INTERVAL 2 DAY),
    (@st_100, @p_towels,  140, 6, 30, 90, 12, 300, NOW(6) - INTERVAL 3 DAY),
    -- ST-101 Northside Express
    (@st_101, @p_bev1,     55, 2, 12, 36,  6, 120, NOW(6) - INTERVAL 2 DAY),
    (@st_101, @p_bev2,     38, 1,  9, 24,  5,  90, NOW(6) - INTERVAL 2 DAY),
    (@st_101, @p_snk1,     64, 3, 15, 45,  6, 150, NOW(6) - INTERVAL 2 DAY),
    (@st_101, @p_snk2,     42, 2, 11, 30,  5, 110, NOW(6) - INTERVAL 2 DAY),
    (@st_101, @p_milk,     50, 2, 12, 48,  9, 120, NOW(6) - INTERVAL 1 DAY),
    (@st_101, @p_yog,      34, 1,  9, 27,  5,  90, NOW(6) - INTERVAL 1 DAY),
    (@st_101, @p_bread,    30, 1,  9, 24,  3,  72, NOW(6) - INTERVAL 1 DAY),
    (@st_101, @p_pizza,    28, 1,  7, 22,  4,  72, NOW(6) - INTERVAL 2 DAY),
    (@st_101, @p_icecream,  6, 0, 15, 42,  6, 132, NOW(6) - INTERVAL 2 DAY),   -- LOW STOCK, seasonal
    (@st_101, @p_towels,   70, 3, 18, 54,  7, 180, NOW(6) - INTERVAL 3 DAY),
    -- ST-200 Riverside Market
    (@st_200, @p_bev1,     72, 3, 16, 48,  8, 160, NOW(6) - INTERVAL 2 DAY),
    (@st_200, @p_bev2,     50, 2, 12, 32,  6, 120, NOW(6) - INTERVAL 2 DAY),
    (@st_200, @p_snk1,     20, 1, 20, 60,  8, 200, NOW(6) - INTERVAL 1 DAY),   -- LOW STOCK, boundary
    (@st_200, @p_snk2,     56, 2, 14, 40,  6, 140, NOW(6) - INTERVAL 2 DAY),
    (@st_200, @p_milk,     64, 3, 16, 64, 12, 160, NOW(6) - INTERVAL 1 DAY),
    (@st_200, @p_yog,      45, 1, 12, 36,  6, 120, NOW(6) - INTERVAL 1 DAY),
    (@st_200, @p_bread,    38, 1, 12, 32,  4,  96, NOW(6) - INTERVAL 1 DAY),
    (@st_200, @p_pizza,    35, 2, 10, 29,  5,  96, NOW(6) - INTERVAL 2 DAY),
    (@st_200, @p_icecream, 68, 3, 20, 56,  8, 176, NOW(6) - INTERVAL 2 DAY),
    (@st_200, @p_towels,   10, 0, 24, 72, 10, 240, NOW(6) - INTERVAL 3 DAY);  -- LOW STOCK, household

-- ============================================================================
-- SECTION 6: SALES HISTORY (14 days, all store/product combinations)
-- Generated from a recursive day sequence, scaled off each row's reorder_qty
-- so faster-moving SKUs show faster-moving sales. Uses a LATERAL derived
-- table so units_sold and gross_revenue are computed from the same draw.
-- ============================================================================

INSERT INTO sales_transaction_daily (store_id, product_id, sales_date, units_sold, gross_revenue)
WITH RECURSIVE seq_days (n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM seq_days WHERE n < 13
)
SELECT
    si.store_id,
    si.product_id,
    DATE_SUB(CURDATE(), INTERVAL sd.n DAY) AS sales_date,
    x.units_sold,
    ROUND(x.units_sold * p.unit_price, 4) AS gross_revenue
FROM store_inventory si
JOIN product p ON p.product_id = si.product_id
CROSS JOIN seq_days sd
CROSS JOIN LATERAL (
    SELECT ROUND(GREATEST(1, (si.reorder_qty / 10) + (RAND() * 4 - 2)), 4) AS units_sold
) x;

-- ============================================================================
-- SECTION 7: AGENT RUN — one ~7 minute run scanning all three stores
-- ============================================================================

SET @agent_run_uuid = UUID();

INSERT INTO agent_run (run_uuid, trigger_type, trigger_source, store_id, started_at, completed_at, status, summary)
VALUES (
    @agent_run_uuid,
    'SCHEDULED',
    'cron:nightly-replenishment-scan',
    NULL,
    NOW(6) - INTERVAL 7 MINUTE,
    NOW(6),
    'COMPLETED',
    'Scanned 3 stores / 10 SKUs. 5 low-stock conditions detected. 1 replenishment order recommended and approved (Whole Milk, ST-100). 1 shortage escalated (Sourdough, ST-100, no supplier mapped). 3 low-stock items left for next cycle.'
);

SET @agent_run_id = LAST_INSERT_ID();

-- ============================================================================
-- SECTION 8: DEMAND FORECASTS
-- Computed from the 14-day sales history just inserted, for the 5 low-stock
-- combinations, 7-day horizon.
-- ============================================================================

INSERT INTO demand_forecast_run (store_id, product_id, forecast_method, forecast_horizon_days, forecasted_demand_qty, confidence_score, generated_at, generated_by_agent_run_id)
SELECT @st_100, @p_milk, 'MOVING_AVG_14D', 7, ROUND(AVG(units_sold) * 7, 4), 0.9000, NOW(6) - INTERVAL 6 MINUTE, @agent_run_id
FROM sales_transaction_daily WHERE store_id = @st_100 AND product_id = @p_milk;

INSERT INTO demand_forecast_run (store_id, product_id, forecast_method, forecast_horizon_days, forecasted_demand_qty, confidence_score, generated_at, generated_by_agent_run_id)
SELECT @st_100, @p_bread, 'MOVING_AVG_14D', 7, ROUND(AVG(units_sold) * 7, 4), 0.6500, NOW(6) - INTERVAL 6 MINUTE, @agent_run_id
FROM sales_transaction_daily WHERE store_id = @st_100 AND product_id = @p_bread;

INSERT INTO demand_forecast_run (store_id, product_id, forecast_method, forecast_horizon_days, forecasted_demand_qty, confidence_score, generated_at, generated_by_agent_run_id)
SELECT @st_101, @p_icecream, 'MOVING_AVG_14D', 7, ROUND(AVG(units_sold) * 7, 4), 0.7200, NOW(6) - INTERVAL 6 MINUTE, @agent_run_id
FROM sales_transaction_daily WHERE store_id = @st_101 AND product_id = @p_icecream;

INSERT INTO demand_forecast_run (store_id, product_id, forecast_method, forecast_horizon_days, forecasted_demand_qty, confidence_score, generated_at, generated_by_agent_run_id)
SELECT @st_200, @p_snk1, 'MOVING_AVG_14D', 7, ROUND(AVG(units_sold) * 7, 4), 0.8800, NOW(6) - INTERVAL 6 MINUTE, @agent_run_id
FROM sales_transaction_daily WHERE store_id = @st_200 AND product_id = @p_snk1;

INSERT INTO demand_forecast_run (store_id, product_id, forecast_method, forecast_horizon_days, forecasted_demand_qty, confidence_score, generated_at, generated_by_agent_run_id)
SELECT @st_200, @p_towels, 'MOVING_AVG_14D', 7, ROUND(AVG(units_sold) * 7, 4), 0.9100, NOW(6) - INTERVAL 6 MINUTE, @agent_run_id
FROM sales_transaction_daily WHERE store_id = @st_200 AND product_id = @p_towels;

-- ============================================================================
-- SECTION 9: AGENT DECISION LOG
-- Scenario A (happy path): Whole Milk @ ST-100 — 4 stages, ends in a
-- recommended order. Scenario B (escalation): Sourdough @ ST-100 — 4 stages,
-- ends in a shortage escalation because no supplier is mapped.
-- ============================================================================

-- Scenario A: Whole Milk @ Downtown Flagship
INSERT INTO agent_decision_log (agent_run_id, stage_name, store_id, product_id, input_snapshot, output_decision, llm_model, llm_rationale, executed_at, duration_ms) VALUES
    (@agent_run_id, 'DETECT_LOW_INVENTORY', @st_100, @p_milk,
        JSON_OBJECT('store_code', 'ST-100', 'sku', 'SKU-DRY-001', 'on_hand_qty', 8, 'allocated_qty', 1, 'reorder_point', 20),
        JSON_OBJECT('flagged', true, 'available_qty', 7, 'deficit', 13),
        NULL, NULL, NOW(6) - INTERVAL 7 MINUTE, 120),
    (@agent_run_id, 'FORECAST_DEMAND', @st_100, @p_milk,
        JSON_OBJECT('store_code', 'ST-100', 'sku', 'SKU-DRY-001', 'lookback_days', 14),
        JSON_OBJECT('forecast_method', 'MOVING_AVG_14D', 'horizon_days', 7, 'confidence_score', 0.90),
        NULL, NULL, NOW(6) - INTERVAL 6 MINUTE - INTERVAL 30 SECOND, 340),
    (@agent_run_id, 'CHECK_SUPPLIER_AVAILABILITY', @st_100, @p_milk,
        JSON_OBJECT('sku', 'SKU-DRY-001', 'candidate_suppliers', JSON_ARRAY('SUP-003')),
        JSON_OBJECT('supplier_code', 'SUP-003', 'lead_time_days', 1, 'reliability_score', 0.90, 'available', true),
        NULL, NULL, NOW(6) - INTERVAL 6 MINUTE, 210),
    (@agent_run_id, 'RECOMMEND_REPLENISHMENT', @st_100, @p_milk,
        JSON_OBJECT('sku', 'SKU-DRY-001', 'deficit', 13, 'forecasted_demand_qty', 44.8, 'safety_stock_qty', 15, 'reorder_qty', 80, 'supplier_code', 'SUP-003', 'is_critical', true),
        JSON_OBJECT('action', 'CREATE_ORDER', 'ordered_qty', 80, 'auto_approved', true, 'reason', 'Critical SKU below reorder point with reliable single-day supplier lead time'),
        'claude-sonnet-4-6', 'SKU is flagged business-critical and the mapped supplier has a same-day lead time and 90% reliability; recommend full reorder quantity with auto-approval rather than routing for manual review.', NOW(6) - INTERVAL 5 MINUTE - INTERVAL 30 SECOND, 980);

-- Scenario B: Sourdough @ Downtown Flagship
INSERT INTO agent_decision_log (agent_run_id, stage_name, store_id, product_id, input_snapshot, output_decision, llm_model, llm_rationale, executed_at, duration_ms) VALUES
    (@agent_run_id, 'DETECT_LOW_INVENTORY', @st_100, @p_bread,
        JSON_OBJECT('store_code', 'ST-100', 'sku', 'SKU-BAK-001', 'on_hand_qty', 5, 'allocated_qty', 0, 'reorder_point', 15),
        JSON_OBJECT('flagged', true, 'available_qty', 5, 'deficit', 10),
        NULL, NULL, NOW(6) - INTERVAL 7 MINUTE, 110),
    (@agent_run_id, 'FORECAST_DEMAND', @st_100, @p_bread,
        JSON_OBJECT('store_code', 'ST-100', 'sku', 'SKU-BAK-001', 'lookback_days', 14),
        JSON_OBJECT('forecast_method', 'MOVING_AVG_14D', 'horizon_days', 7, 'confidence_score', 0.65),
        NULL, NULL, NOW(6) - INTERVAL 6 MINUTE - INTERVAL 30 SECOND, 305),
    (@agent_run_id, 'CHECK_SUPPLIER_AVAILABILITY', @st_100, @p_bread,
        JSON_OBJECT('sku', 'SKU-BAK-001', 'candidate_suppliers', JSON_ARRAY()),
        JSON_OBJECT('supplier_code', NULL, 'available', false, 'reason', 'No supplier_product mapping exists for this SKU'),
        NULL, NULL, NOW(6) - INTERVAL 6 MINUTE, 95),
    (@agent_run_id, 'ESCALATE_SHORTAGE', @st_100, @p_bread,
        JSON_OBJECT('sku', 'SKU-BAK-001', 'deficit', 10, 'shelf_life_days', 3, 'supplier_available', false),
        JSON_OBJECT('action', 'ESCALATE', 'severity', 'HIGH', 'reason', 'SUPPLIER_UNAVAILABLE'),
        'llama-3.3-70b', 'No supplier is mapped for this bakery SKU and it has a 3-day shelf life, so waiting for the next scan risks a stockout before any manual sourcing could complete. Escalating as HIGH rather than MEDIUM given the shelf-life constraint.', NOW(6) - INTERVAL 5 MINUTE - INTERVAL 45 SECOND, 860);

-- ============================================================================
-- SECTION 10: LLM CALL AUDIT
-- Linked to the two decisions above that actually invoked an LLM (the
-- deterministic detect/forecast/check stages did not).
-- ============================================================================

INSERT INTO llm_call_log (agent_run_id, decision_id, provider_code, model_name, request_payload, response_payload, prompt_tokens, completion_tokens, estimated_cost_usd, latency_ms, http_status_code, is_success, requested_at, completed_at)
SELECT
    @agent_run_id,
    decision_id,
    'ANTHROPIC',
    'claude-sonnet-4-6',
    JSON_OBJECT('model', 'claude-sonnet-4-6', 'max_tokens', 500, 'messages', JSON_ARRAY(
        JSON_OBJECT('role', 'user', 'content', 'Given deficit=13, forecasted_demand_qty=44.8, safety_stock_qty=15, reorder_qty=80, supplier lead_time_days=1, reliability=0.90, is_critical=true — recommend a replenishment action.'))),
    JSON_OBJECT('content', JSON_ARRAY(JSON_OBJECT('type', 'text', 'text', 'Recommend CREATE_ORDER for 80 units, auto-approve given critical SKU and reliable single-day lead time.'))),
    186, 64, 0.001560, 940, 200, 1,
    NOW(6) - INTERVAL 5 MINUTE - INTERVAL 30 SECOND, NOW(6) - INTERVAL 5 MINUTE - INTERVAL 29 SECOND
FROM agent_decision_log
WHERE agent_run_id = @agent_run_id AND stage_name = 'RECOMMEND_REPLENISHMENT' AND product_id = @p_milk;

INSERT INTO llm_call_log (agent_run_id, decision_id, provider_code, model_name, request_payload, response_payload, prompt_tokens, completion_tokens, estimated_cost_usd, latency_ms, http_status_code, is_success, requested_at, completed_at)
SELECT
    @agent_run_id,
    decision_id,
    'AZURE_AI_FOUNDRY',
    'llama-3.3-70b',
    JSON_OBJECT('model', 'llama-3.3-70b', 'max_tokens', 400, 'messages', JSON_ARRAY(
        JSON_OBJECT('role', 'user', 'content', 'Given deficit=10, shelf_life_days=3, supplier_available=false — should this be escalated, and at what severity?'))),
    JSON_OBJECT('content', JSON_ARRAY(JSON_OBJECT('type', 'text', 'text', 'Escalate as HIGH severity: no supplier mapped and a 3-day shelf life leaves no margin for the next scheduled scan.'))),
    142, 58, 0.000000, 1120, 200, 1,
    NOW(6) - INTERVAL 5 MINUTE - INTERVAL 45 SECOND, NOW(6) - INTERVAL 5 MINUTE - INTERVAL 44 SECOND
FROM agent_decision_log
WHERE agent_run_id = @agent_run_id AND stage_name = 'ESCALATE_SHORTAGE' AND product_id = @p_bread;

-- ============================================================================
-- SECTION 11: REPLENISHMENT ORDER (Scenario A outcome)
-- ============================================================================

INSERT INTO replenishment_order (order_number, store_id, supplier_id, status_code, source_type, generated_by_agent_run_id, total_cost, requested_delivery_date, approved_by, approved_at, sent_to_supplier_at)
VALUES (
    CONCAT('RO-', DATE_FORMAT(CURDATE(), '%Y'), '-', LPAD(FLOOR(RAND() * 99999), 5, '0')),
    @st_100, @sup_dairy, 'SENT_TO_SUPPLIER', 'AGENT', @agent_run_id,
    184.0000, CURDATE() + INTERVAL 1 DAY,
    'agent-auto-approval', NOW(6) - INTERVAL 5 MINUTE, NOW(6) - INTERVAL 4 MINUTE
);

SET @repl_order_id = LAST_INSERT_ID();

INSERT INTO replenishment_order_line (replenishment_order_id, product_id, ordered_qty, unit_cost, forecast_run_id)
SELECT @repl_order_id, @p_milk, 80, 2.3000, forecast_run_id
FROM demand_forecast_run WHERE store_id = @st_100 AND product_id = @p_milk AND generated_by_agent_run_id = @agent_run_id;

-- ============================================================================
-- SECTION 12: DELIVERY SHIPMENT + TRACKING (Scenario A, in transit)
-- ============================================================================

INSERT INTO delivery_shipment (replenishment_order_id, carrier_name, tracking_number, status_code, shipped_at, estimated_arrival_at)
VALUES (@repl_order_id, 'Midwest Cold Chain Logistics', 'MCC-2026-58421', 'IN_TRANSIT', NOW(6) - INTERVAL 3 MINUTE, NOW(6) + INTERVAL 1 DAY);

SET @shipment_id = LAST_INSERT_ID();

INSERT INTO delivery_tracking_event (shipment_id, event_code, event_at, event_location, notes) VALUES
    (@shipment_id, 'DEPARTED',   NOW(6) - INTERVAL 3 MINUTE, 'FreshChain DC - Joliet, IL', 'Cold chain seal verified at dispatch'),
    (@shipment_id, 'IN_TRANSIT', NOW(6) - INTERVAL 1 MINUTE, 'I-55 Corridor', 'On schedule for next-day delivery');

-- ============================================================================
-- SECTION 13: SHORTAGE ESCALATION (Scenario B outcome)
-- ============================================================================

INSERT INTO shortage_escalation (replenishment_order_id, store_id, product_id, escalation_reason, severity, status_code, raised_by_agent_run_id)
VALUES (NULL, @st_100, @p_bread, 'SUPPLIER_UNAVAILABLE', 'HIGH', 'OPEN', @agent_run_id);
