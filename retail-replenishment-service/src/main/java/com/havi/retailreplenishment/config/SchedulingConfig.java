package com.havi.retailreplenishment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * A dedicated pool for the delivery simulator's per-shipment delayed tasks
 * (DEPARTED / IN_TRANSIT / OUT_FOR_DELIVERY / DELIVERED), separate from
 * whatever default scheduler @EnableScheduling would otherwise wire up, so
 * a burst of shipments in one agent run doesn't starve other scheduled work.
 */
@Configuration
public class SchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler deliverySimulatorTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("delivery-sim-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}
