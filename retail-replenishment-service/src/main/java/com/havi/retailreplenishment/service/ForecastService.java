package com.havi.retailreplenishment.service;

import com.havi.retailreplenishment.dto.DemandForecastDto;
import com.havi.retailreplenishment.dto.ForecastRequest;
import com.havi.retailreplenishment.entity.*;
import com.havi.retailreplenishment.exception.ResourceNotFoundException;
import com.havi.retailreplenishment.mapper.DemandForecastMapper;
import com.havi.retailreplenishment.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Backs the "forecast demand" stage. Deliberately a simple moving average,
 * not a fitted model — per the architecture note, the LLM's job is judgment
 * (interpreting the forecast), not arithmetic, so this stays deterministic
 * and auditable.
 */
@Service
@Transactional
public class ForecastService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final SalesTransactionDailyRepository salesRepository;
    private final DemandForecastRunRepository forecastRunRepository;
    private final AgentRunRepository agentRunRepository;

    public ForecastService(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            SalesTransactionDailyRepository salesRepository,
            DemandForecastRunRepository forecastRunRepository,
            AgentRunRepository agentRunRepository) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.salesRepository = salesRepository;
        this.forecastRunRepository = forecastRunRepository;
        this.agentRunRepository = agentRunRepository;
    }

    public DemandForecastDto computeMovingAverageForecast(ForecastRequest request) {
        Store store = storeRepository.findById(request.storeId())
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + request.storeId()));
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));

        int lookbackDays = request.lookbackDays() != null ? request.lookbackDays() : 14;
        int horizonDays = request.horizonDays() != null ? request.horizonDays() : 7;

        List<SalesTransactionDaily> history = salesRepository.findRecentHistory(
            store.getStoreId(), product.getProductId(), LocalDate.now().minusDays(lookbackDays));

        BigDecimal avgDailyUnits;
        BigDecimal confidence;
        if (history.isEmpty()) {
            avgDailyUnits = BigDecimal.ZERO;
            confidence = BigDecimal.ZERO;
        } else {
            BigDecimal total = history.stream()
                .map(SalesTransactionDaily::getUnitsSold)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgDailyUnits = total.divide(BigDecimal.valueOf(history.size()), 4, RoundingMode.HALF_UP);
            // Confidence scales with how much of the lookback window actually
            // has data — sparse history (new SKU, new store) yields a lower
            // confidence score rather than a silently overconfident number.
            BigDecimal coverage = BigDecimal.valueOf(history.size())
                .divide(BigDecimal.valueOf(lookbackDays), 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
            confidence = coverage.multiply(BigDecimal.valueOf(0.9)).setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal forecastedDemandQty = avgDailyUnits.multiply(BigDecimal.valueOf(horizonDays))
            .setScale(4, RoundingMode.HALF_UP);

        DemandForecastRun run = new DemandForecastRun();
        run.setStore(store);
        run.setProduct(product);
        run.setForecastMethod("MOVING_AVG_" + lookbackDays + "D");
        run.setForecastHorizonDays(horizonDays);
        run.setForecastedDemandQty(forecastedDemandQty);
        run.setConfidenceScore(confidence);
        run.setGeneratedAt(LocalDateTime.now());
        if (request.generatedByAgentRunId() != null) {
            AgentRun agentRun = agentRunRepository.findById(request.generatedByAgentRunId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent run not found: " + request.generatedByAgentRunId()));
            run.setGeneratedByAgentRun(agentRun);
        }

        DemandForecastRun saved = forecastRunRepository.save(run);
        return DemandForecastMapper.toDto(saved);
    }

    public DemandForecastDto latest(Long storeId, Long productId) {
        return forecastRunRepository.findLatest(storeId, productId)
            .map(DemandForecastMapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No forecast exists yet for store " + storeId + " / product " + productId));
    }
}
