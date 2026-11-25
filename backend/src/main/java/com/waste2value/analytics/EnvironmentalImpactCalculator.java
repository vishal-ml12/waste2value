package com.waste2value.analytics;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Reusable, independently testable environmental calculation engine.
 * Computes estimated avoided CO2 emissions and landfill diversion
 * based on eligible processed/recycled waste quantities and authoritative emission factors.
 */
@Component
public class EnvironmentalImpactCalculator {

    public static final String ESTIMATE_DISCLAIMER =
            "All CO2 and landfill metrics are estimates derived from published life-cycle assessment models (US EPA WARM v15 and UK DEFRA). Actual environmental benefits may vary depending on local processing methods, transport distances, and energy grids.";

    public static final String METHODOLOGY_REFERENCE =
            "Avoided burden approach (ISO 14044): Recycled material avoids primary virgin extraction, refining, and fossil-fuel energy expenditure, net of secondary reprocessing energy and avoided municipal landfill methane generation.";

    public record MaterialBatch(Long categoryId, String categoryName, BigDecimal quantityKg) {}

    public record CategoryImpact(
            Long categoryId,
            String categoryName,
            BigDecimal quantityProcessedKg,
            BigDecimal co2FactorKgPerUnit,
            BigDecimal landfillDiversionRate,
            BigDecimal co2AvoidedKg,
            BigDecimal landfillDivertedKg,
            String sourceReference,
            boolean isFactorConfigured
    ) {}

    public record ImpactSummary(
            BigDecimal totalProcessedKg,
            BigDecimal totalCo2AvoidedKg,
            BigDecimal totalCo2AvoidedMt,
            BigDecimal totalLandfillDivertedKg,
            List<CategoryImpact> categoryBreakdown,
            List<String> unconfiguredCategories,
            String methodology,
            String disclaimer
    ) {}

    /**
     * Calculates environmental impact summary from a collection of material batches and an emission factor provider.
     */
    public ImpactSummary calculateImpact(
            List<MaterialBatch> batches,
            Map<Long, EnvironmentalEmissionFactor> factorMap
    ) {
        if (batches == null || batches.isEmpty()) {
            return new ImpactSummary(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    METHODOLOGY_REFERENCE,
                    ESTIMATE_DISCLAIMER
            );
        }

        // Group batches by category id to avoid duplicate rows
        Map<Long, MaterialBatchAggregator> aggregated = new LinkedHashMap<>();
        for (MaterialBatch batch : batches) {
            if (batch.quantityKg() == null || batch.quantityKg().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            aggregated.computeIfAbsent(batch.categoryId(), id -> new MaterialBatchAggregator(id, batch.categoryName()))
                    .addQuantity(batch.quantityKg());
        }

        BigDecimal totalProcessed = BigDecimal.ZERO;
        BigDecimal totalCo2Avoided = BigDecimal.ZERO;
        BigDecimal totalLandfillDiverted = BigDecimal.ZERO;
        boolean hasAnyConfiguredCo2 = false;
        List<CategoryImpact> breakdown = new ArrayList<>();
        List<String> unconfiguredList = new ArrayList<>();

        for (MaterialBatchAggregator agg : aggregated.values()) {
            BigDecimal qty = agg.totalQuantity.setScale(2, RoundingMode.HALF_UP);
            totalProcessed = totalProcessed.add(qty);

            EnvironmentalEmissionFactor factor = factorMap.get(agg.categoryId);

            boolean isConfigured = factor != null && factor.getCo2FactorKgPerUnit() != null;
            BigDecimal co2Factor = isConfigured ? factor.getCo2FactorKgPerUnit() : null;
            BigDecimal diversionRate = factor != null && factor.getLandfillDiversionRate() != null
                    ? factor.getLandfillDiversionRate()
                    : BigDecimal.ONE;

            BigDecimal co2Avoided = null;
            if (isConfigured) {
                co2Avoided = qty.multiply(co2Factor).setScale(2, RoundingMode.HALF_UP);
                totalCo2Avoided = totalCo2Avoided.add(co2Avoided);
                hasAnyConfiguredCo2 = true;
            } else {
                unconfiguredList.add(agg.categoryName);
            }

            BigDecimal landfillDiverted = qty.multiply(diversionRate).setScale(2, RoundingMode.HALF_UP);
            totalLandfillDiverted = totalLandfillDiverted.add(landfillDiverted);

            String source = factor != null ? factor.getSourceReference() : "Unconfigured";

            breakdown.add(new CategoryImpact(
                    agg.categoryId,
                    agg.categoryName,
                    qty,
                    co2Factor,
                    diversionRate,
                    co2Avoided,
                    landfillDiverted,
                    source,
                    isConfigured
            ));
        }

        BigDecimal totalCo2Mt = hasAnyConfiguredCo2
                ? totalCo2Avoided.divide(new BigDecimal("1000.00"), 3, RoundingMode.HALF_UP)
                : null;

        BigDecimal reportedTotalCo2 = hasAnyConfiguredCo2
                ? totalCo2Avoided.setScale(2, RoundingMode.HALF_UP)
                : null;

        return new ImpactSummary(
                totalProcessed.setScale(2, RoundingMode.HALF_UP),
                reportedTotalCo2,
                totalCo2Mt,
                totalLandfillDiverted.setScale(2, RoundingMode.HALF_UP),
                breakdown,
                unconfiguredList,
                METHODOLOGY_REFERENCE,
                ESTIMATE_DISCLAIMER
        );
    }

    private static class MaterialBatchAggregator {
        final Long categoryId;
        final String categoryName;
        BigDecimal totalQuantity = BigDecimal.ZERO;

        MaterialBatchAggregator(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        void addQuantity(BigDecimal qty) {
            this.totalQuantity = this.totalQuantity.add(qty);
        }
    }
}
