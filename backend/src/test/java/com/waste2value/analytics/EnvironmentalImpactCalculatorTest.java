package com.waste2value.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentalImpactCalculatorTest {

    private EnvironmentalImpactCalculator calculator;
    private EnvironmentalEmissionFactor aluminiumFactor;
    private EnvironmentalEmissionFactor petFactor;
    private EnvironmentalEmissionFactor mixedPlasticFactor;
    private EnvironmentalEmissionFactor eWasteFactor;

    @BeforeEach
    void setUp() {
        calculator = new EnvironmentalImpactCalculator();

        aluminiumFactor = new EnvironmentalEmissionFactor(
                null, new BigDecimal("9.1200"), new BigDecimal("1.0000"), "kg",
                "US EPA WARM v15 (Aluminium)", "Secondary smelting energy reduction",
                LocalDate.of(2024, 1, 1), null, true
        );

        petFactor = new EnvironmentalEmissionFactor(
                null, new BigDecimal("1.5300"), new BigDecimal("1.0000"), "kg",
                "US EPA WARM v15 (PET)", "Displacing virgin polyester feedstock",
                LocalDate.of(2024, 1, 1), null, true
        );

        mixedPlasticFactor = new EnvironmentalEmissionFactor(
                null, new BigDecimal("1.0200"), new BigDecimal("0.9000"), "kg",
                "US EPA WARM v15 (Mixed Plastic)", "Mechanical recycling with 10% processing residue",
                LocalDate.of(2024, 1, 1), null, true
        );

        eWasteFactor = new EnvironmentalEmissionFactor(
                null, null, new BigDecimal("0.8500"), "items",
                "CPCB Guidelines (Unconfigured Factor)", "High variance per item",
                LocalDate.of(2024, 1, 1), null, true
        );
    }

    @Test
    @DisplayName("Calculate CO2 avoided and landfill diversion accurately for configured materials")
    void calculateAccurateImpactForConfiguredMaterials() {
        Map<Long, EnvironmentalEmissionFactor> factorMap = Map.of(
                13L, aluminiumFactor, // 100 kg Aluminium
                8L, petFactor         // 200 kg PET Bottles
        );

        List<EnvironmentalImpactCalculator.MaterialBatch> batches = List.of(
                new EnvironmentalImpactCalculator.MaterialBatch(13L, "Aluminium", new BigDecimal("100.00")),
                new EnvironmentalImpactCalculator.MaterialBatch(8L, "PET Bottles", new BigDecimal("200.00"))
        );

        EnvironmentalImpactCalculator.ImpactSummary summary = calculator.calculateImpact(batches, factorMap);

        // 100 kg * 9.12 = 912.00 kg CO2e; 200 kg * 1.53 = 306.00 kg CO2e -> Total = 1218.00 kg CO2e
        assertThat(summary.totalProcessedKg()).isEqualTo(new BigDecimal("300.00"));
        assertThat(summary.totalCo2AvoidedKg()).isEqualTo(new BigDecimal("1218.00"));
        // 1218.00 kg = 1.218 Metric Tons
        assertThat(summary.totalCo2AvoidedMt()).isEqualTo(new BigDecimal("1.218"));
        // 100% diversion: 100 + 200 = 300 kg
        assertThat(summary.totalLandfillDivertedKg()).isEqualTo(new BigDecimal("300.00"));
        assertThat(summary.unconfiguredCategories()).isEmpty();
        assertThat(summary.disclaimer()).contains("estimates");
    }

    @Test
    @DisplayName("Calculates partial landfill diversion rate accurately when processing residue exists")
    void calculatesPartialLandfillDiversionRate() {
        Map<Long, EnvironmentalEmissionFactor> factorMap = Map.of(
                10L, mixedPlasticFactor // 0.90 diversion rate, 1.02 CO2 factor
        );

        List<EnvironmentalImpactCalculator.MaterialBatch> batches = List.of(
                new EnvironmentalImpactCalculator.MaterialBatch(10L, "Mixed Plastic", new BigDecimal("500.00"))
        );

        EnvironmentalImpactCalculator.ImpactSummary summary = calculator.calculateImpact(batches, factorMap);

        // 500 kg * 1.02 = 510.00 kg CO2e
        assertThat(summary.totalCo2AvoidedKg()).isEqualTo(new BigDecimal("510.00"));
        // 500 kg * 0.90 diversion = 450.00 kg diverted from landfill
        assertThat(summary.totalLandfillDivertedKg()).isEqualTo(new BigDecimal("450.00"));
    }

    @Test
    @DisplayName("Safely handles unconfigured categories without fabricating CO2 figures")
    void safelyHandlesUnconfiguredCategory() {
        Map<Long, EnvironmentalEmissionFactor> factorMap = Map.of(
                5L, eWasteFactor // Factor is NULL
        );

        List<EnvironmentalImpactCalculator.MaterialBatch> batches = List.of(
                new EnvironmentalImpactCalculator.MaterialBatch(5L, "E-Waste", new BigDecimal("50.00"))
        );

        EnvironmentalImpactCalculator.ImpactSummary summary = calculator.calculateImpact(batches, factorMap);

        assertThat(summary.totalProcessedKg()).isEqualTo(new BigDecimal("50.00"));
        // Since no configured CO2 factor exists, CO2 is null (unavailable), not fabricated as 0.00
        assertThat(summary.totalCo2AvoidedKg()).isNull();
        assertThat(summary.totalCo2AvoidedMt()).isNull();
        // Landfill diversion rate of 0.85 applies: 50 * 0.85 = 42.50 kg
        assertThat(summary.totalLandfillDivertedKg()).isEqualTo(new BigDecimal("42.50"));
        assertThat(summary.unconfiguredCategories()).contains("E-Waste");

        EnvironmentalImpactCalculator.CategoryImpact catImpact = summary.categoryBreakdown().get(0);
        assertThat(catImpact.isFactorConfigured()).isFalse();
        assertThat(catImpact.co2AvoidedKg()).isNull();
        assertThat(catImpact.sourceReference()).contains("Unconfigured");
    }

    @Test
    @DisplayName("Safely processes mixed batches with both configured and unconfigured items")
    void processesMixedBatchesTransparently() {
        Map<Long, EnvironmentalEmissionFactor> factorMap = Map.of(
                8L, petFactor,
                5L, eWasteFactor
        );

        List<EnvironmentalImpactCalculator.MaterialBatch> batches = List.of(
                new EnvironmentalImpactCalculator.MaterialBatch(8L, "PET Bottles", new BigDecimal("100.00")),
                new EnvironmentalImpactCalculator.MaterialBatch(5L, "E-Waste", new BigDecimal("40.00"))
        );

        EnvironmentalImpactCalculator.ImpactSummary summary = calculator.calculateImpact(batches, factorMap);

        // PET CO2 is counted (100 * 1.53 = 153.00 kg)
        assertThat(summary.totalCo2AvoidedKg()).isEqualTo(new BigDecimal("153.00"));
        assertThat(summary.totalCo2AvoidedMt()).isEqualTo(new BigDecimal("0.153"));
        // Landfill diversion: (100 * 1.0) + (40 * 0.85) = 100 + 34 = 134.00 kg
        assertThat(summary.totalLandfillDivertedKg()).isEqualTo(new BigDecimal("134.00"));
        assertThat(summary.unconfiguredCategories()).containsExactly("E-Waste");
    }

    @Test
    @DisplayName("Handles empty batch list safely without error")
    void handlesEmptyBatchListSafely() {
        EnvironmentalImpactCalculator.ImpactSummary summary = calculator.calculateImpact(List.of(), Map.of());

        assertThat(summary.totalProcessedKg()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.totalCo2AvoidedKg()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.totalCo2AvoidedMt()).isEqualTo(new BigDecimal("0.000"));
        assertThat(summary.totalLandfillDivertedKg()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.categoryBreakdown()).isEmpty();
        assertThat(summary.unconfiguredCategories()).isEmpty();
    }
}
