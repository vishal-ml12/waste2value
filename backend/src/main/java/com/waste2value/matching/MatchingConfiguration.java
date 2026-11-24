package com.waste2value.matching;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurable weights and thresholds for the Waste2Value hybrid recommendation engine.
 * All weights in each category sum to 1.0 (100%).
 */
@Configuration
@ConfigurationProperties(prefix = "app.matching")
public class MatchingConfiguration {

    // Collector Scoring Weights (Total = 1.0)
    private double collectorDistanceWeight = 0.35;
    private double collectorServiceAreaWeight = 0.25;
    private double collectorPerformanceWeight = 0.20;
    private double collectorRatingWeight = 0.20;

    // Processor Scoring Weights (Total = 1.0)
    private double processorCapacityWeight = 0.30;
    private double processorCapabilityWeight = 0.25;
    private double processorQuantityWeight = 0.20;
    private double processorDistanceWeight = 0.15;
    private double processorRatingWeight = 0.10;

    // Benchmark thresholds
    private int collectorPerformanceBenchmarkPickups = 20;
    private double defaultProcessorRadiusKm = 100.0;
    private double unratedScoreBaseline = 60.0;
    private double missingDistanceScoreBaseline = 50.0;

    public MatchingConfiguration() {}

    public double getCollectorDistanceWeight() {
        return collectorDistanceWeight;
    }

    public void setCollectorDistanceWeight(double collectorDistanceWeight) {
        this.collectorDistanceWeight = collectorDistanceWeight;
    }

    public double getCollectorServiceAreaWeight() {
        return collectorServiceAreaWeight;
    }

    public void setCollectorServiceAreaWeight(double collectorServiceAreaWeight) {
        this.collectorServiceAreaWeight = collectorServiceAreaWeight;
    }

    public double getCollectorPerformanceWeight() {
        return collectorPerformanceWeight;
    }

    public void setCollectorPerformanceWeight(double collectorPerformanceWeight) {
        this.collectorPerformanceWeight = collectorPerformanceWeight;
    }

    public double getCollectorRatingWeight() {
        return collectorRatingWeight;
    }

    public void setCollectorRatingWeight(double collectorRatingWeight) {
        this.collectorRatingWeight = collectorRatingWeight;
    }

    public double getProcessorCapacityWeight() {
        return processorCapacityWeight;
    }

    public void setProcessorCapacityWeight(double processorCapacityWeight) {
        this.processorCapacityWeight = processorCapacityWeight;
    }

    public double getProcessorCapabilityWeight() {
        return processorCapabilityWeight;
    }

    public void setProcessorCapabilityWeight(double processorCapabilityWeight) {
        this.processorCapabilityWeight = processorCapabilityWeight;
    }

    public double getProcessorQuantityWeight() {
        return processorQuantityWeight;
    }

    public void setProcessorQuantityWeight(double processorQuantityWeight) {
        this.processorQuantityWeight = processorQuantityWeight;
    }

    public double getProcessorDistanceWeight() {
        return processorDistanceWeight;
    }

    public void setProcessorDistanceWeight(double processorDistanceWeight) {
        this.processorDistanceWeight = processorDistanceWeight;
    }

    public double getProcessorRatingWeight() {
        return processorRatingWeight;
    }

    public void setProcessorRatingWeight(double processorRatingWeight) {
        this.processorRatingWeight = processorRatingWeight;
    }

    public int getCollectorPerformanceBenchmarkPickups() {
        return collectorPerformanceBenchmarkPickups;
    }

    public void setCollectorPerformanceBenchmarkPickups(int collectorPerformanceBenchmarkPickups) {
        this.collectorPerformanceBenchmarkPickups = collectorPerformanceBenchmarkPickups;
    }

    public double getDefaultProcessorRadiusKm() {
        return defaultProcessorRadiusKm;
    }

    public void setDefaultProcessorRadiusKm(double defaultProcessorRadiusKm) {
        this.defaultProcessorRadiusKm = defaultProcessorRadiusKm;
    }

    public double getUnratedScoreBaseline() {
        return unratedScoreBaseline;
    }

    public void setUnratedScoreBaseline(double unratedScoreBaseline) {
        this.unratedScoreBaseline = unratedScoreBaseline;
    }

    public double getMissingDistanceScoreBaseline() {
        return missingDistanceScoreBaseline;
    }

    public void setMissingDistanceScoreBaseline(double missingDistanceScoreBaseline) {
        this.missingDistanceScoreBaseline = missingDistanceScoreBaseline;
    }
}
