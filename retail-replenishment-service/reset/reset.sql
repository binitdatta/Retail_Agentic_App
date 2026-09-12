-- ============================================================================
-- Demo reset — restores the original 5 low-stock scenarios and wipes all
-- transactional/agent activity, WITHOUT touching master data (store/product/
-- supplier) or the 14-day sales history the forecast stage reads from.
-- Safe to run repeatedly, right before each demo/rehearsal.
--
-- Run time: instant (no 420-row regeneration like the full V2 reseed).
-- ============================================================================

USE retail_replenishment;

SET FOREIGN_KEY_CHECKS = 0;

-- ---- 1. Clear everything created by agent runs / manual testing ----------
TRUNCATE TABLE llm_call_log;
TRUNCATE TABLE agent_decision_log;
TRUNCATE TABLE agent_run;
TRUNCATE TABLE delivery_tracking_event;
TRUNCATE TABLE delivery_shipment;
TRUNCATE TABLE replenishment_order_line;
TRUNCATE TABLE replenishment_order;
TRUNCATE TABLE shortage_escalation;
TRUNCATE TABLE demand_forecast_run;

SET FOREIGN_KEY_CHECKS = 1;

-- ---- 2. Restore store_inventory to the original 5 low-stock scenarios ----
-- (Values match V2__seed_test_data.sql exactly.)

UPDATE store_inventory si
    JOIN store s ON s.store_id = si.store_id
    JOIN product p ON p.product_id = si.product_id
    SET si.on_hand_qty = 8, si.allocated_qty = 1
WHERE s.store_code = 'ST-100' AND p.sku_code = 'SKU-DRY-001';   -- Whole Milk

UPDATE store_inventory si
    JOIN store s ON s.store_id = si.store_id
    JOIN product p ON p.product_id = si.product_id
    SET si.on_hand_qty = 5, si.allocated_qty = 0
WHERE s.store_code = 'ST-100' AND p.sku_code = 'SKU-BAK-001';   -- Sourdough

UPDATE store_inventory si
    JOIN store s ON s.store_id = si.store_id
    JOIN product p ON p.product_id = si.product_id
    SET si.on_hand_qty = 6, si.allocated_qty = 0
WHERE s.store_code = 'ST-101' AND p.sku_code = 'SKU-FRZ-002';   -- Ice Cream

UPDATE store_inventory si
    JOIN store s ON s.store_id = si.store_id
    JOIN product p ON p.product_id = si.product_id
    SET si.on_hand_qty = 20, si.allocated_qty = 1
WHERE s.store_code = 'ST-200' AND p.sku_code = 'SKU-SNK-001';   -- Kettle Chips

UPDATE store_inventory si
    JOIN store s ON s.store_id = si.store_id
    JOIN product p ON p.product_id = si.product_id
    SET si.on_hand_qty = 10, si.allocated_qty = 0
WHERE s.store_code = 'ST-200' AND p.sku_code = 'SKU-HH-001';    -- Paper Towels

-- ---- 3. Verify ---------------------------------------------------------
SELECT * FROM vw_low_stock_inventory;   -- should return exactly 5 rows again