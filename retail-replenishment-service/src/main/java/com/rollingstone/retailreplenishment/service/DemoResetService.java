package com.rollingstone.retailreplenishment.service;

import com.rollingstone.retailreplenishment.exception.DemoResetDisabledException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Replaces the manually-run reset_demo_scenario.sql: restores the original
 * 5 low-stock scenarios and wipes all transactional/agent activity,
 * without touching master data or the 14-day sales history the forecast
 * stage reads from. Same script, callable from the dashboard instead of
 * MySQL Workbench.
 *
 * SAFETY: disabled unless app.demo-reset.enabled=true is explicitly set —
 * see application.yml. This truncates real transactional tables; the
 * default must stay false (or absent) in any configuration that isn't a
 * local demo/rehearsal environment. Never flip this on in a shared or
 * production profile.
 *
 * TRUNCATE causes an implicit commit in InnoDB, so this is not atomic in
 * the way a normal @Transactional method would be — a failure partway
 * through leaves a partially-reset database, same as running the SQL
 * script by hand would. Re-running it is safe either way (every statement
 * here is idempotent).
 */
@Service
public class DemoResetService {

    // Child tables first — TRUNCATE ignores FK cascade ordering even with
    // checks disabled, so explicit ordering keeps this readable even
    // though FOREIGN_KEY_CHECKS=0 would technically let any order work.
    // llm_call_http_trace is NOT in the original manual script — it didn't
    // exist yet when that script was written. Left out, a reset would
    // leave orphaned trace rows pointing at deleted llm_call_log rows.
    private static final List<String> TABLES_TO_TRUNCATE = List.of(
            "llm_call_http_trace",
            "llm_call_log",
            "agent_decision_log",
            "agent_run",
            "delivery_tracking_event",
            "delivery_shipment",
            "replenishment_order_line",
            "replenishment_order",
            "shortage_escalation",
            "demand_forecast_run"
    );

    private record LowStockScenario(String storeCode, String skuCode, double onHandQty, double allocatedQty) {}

    // Values match V2__seed_test_data.sql exactly.
    private static final List<LowStockScenario> SCENARIOS = List.of(
            new LowStockScenario("ST-100", "SKU-DRY-001", 8, 1),   // Whole Milk
            new LowStockScenario("ST-100", "SKU-BAK-001", 5, 0),   // Sourdough
            new LowStockScenario("ST-101", "SKU-FRZ-002", 6, 0),   // Ice Cream
            new LowStockScenario("ST-200", "SKU-SNK-001", 20, 1),  // Kettle Chips
            new LowStockScenario("ST-200", "SKU-HH-001", 10, 0)    // Paper Towels
    );

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public DemoResetService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.demo-reset.enabled:false}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @Transactional
    public void resetToDemoScenario() {
        if (!enabled) {
            throw new DemoResetDisabledException(
                    "Demo reset is disabled in this environment (app.demo-reset.enabled is not true). " +
                            "This is a safety default, not a bug — enable it only in a local/demo configuration.");
        }

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : TABLES_TO_TRUNCATE) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        for (LowStockScenario s : SCENARIOS) {
            jdbcTemplate.update("""
                UPDATE store_inventory si
                JOIN store st ON st.store_id = si.store_id
                JOIN product p ON p.product_id = si.product_id
                SET si.on_hand_qty = ?, si.allocated_qty = ?
                WHERE st.store_code = ? AND p.sku_code = ?
                """,
                    s.onHandQty(), s.allocatedQty(), s.storeCode(), s.skuCode());
        }
    }
}