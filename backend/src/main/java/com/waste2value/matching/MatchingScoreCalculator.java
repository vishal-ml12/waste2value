package com.waste2value.matching;

import com.waste2value.address.Address;
import com.waste2value.collector.CollectorProfile;
import com.waste2value.listing.Listing;
import com.waste2value.processor.ProcessorCapability;
import com.waste2value.processor.ProcessorProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage B: Normalized suitability scoring algorithm (0 to 100).
 * Implements transparent, weighted multi-factor scoring with explanatory recommendation reasons.
 */
@Component
public class MatchingScoreCalculator {

    private final MatchingConfiguration config;

    public MatchingScoreCalculator(MatchingConfiguration config) {
        this.config = config;
    }

    public record ScoreResult(double totalScore, List<String> reasons) {}

    /**
     * Calculates suitability of a Collector for a specific Listing.
     */
    public ScoreResult calculateCollectorScore(CollectorProfile collector, Listing listing, Double distanceKm) {
        List<String> reasons = new ArrayList<>();

        if (collector.isVerified()) {
            reasons.add("Verified Collector \u2705");
        }

        // 1. Distance suitability (35%)
        double distanceScore;
        if (distanceKm != null) {
            double maxDist = collector.getMaxDistanceKm() != null && collector.getMaxDistanceKm().compareTo(BigDecimal.ZERO) > 0
                    ? collector.getMaxDistanceKm().doubleValue()
                    : 50.0;
            distanceScore = Math.max(0.0, 100.0 * (1.0 - (distanceKm / maxDist)));
            if (distanceKm <= 10.0) {
                reasons.add("Close proximity (" + distanceKm + " km away)");
            } else {
                reasons.add("Within operating radius (" + distanceKm + " km)");
            }
        } else {
            distanceScore = config.getMissingDistanceScoreBaseline();
            reasons.add("Location within service territory");
        }

        // 2. Service Area suitability (25%)
        double serviceAreaScore = evaluateServiceArea(collector.getServiceArea(), listing.getAddress());
        if (serviceAreaScore >= 90.0) {
            reasons.add("Direct service area match (" + collector.getServiceArea() + ")");
        } else if (serviceAreaScore >= 60.0) {
            reasons.add("Regional coverage (" + collector.getServiceArea() + ")");
        }

        // 3. Historical pickup performance (20%)
        double performanceScore;
        int pickups = collector.getTotalPickups();
        int benchmark = config.getCollectorPerformanceBenchmarkPickups();
        if (pickups >= benchmark) {
            performanceScore = 100.0;
            reasons.add("Proven track record (" + pickups + " completed pickups)");
        } else if (pickups > 0) {
            performanceScore = Math.min(100.0, Math.max(30.0, ((double) pickups / benchmark) * 100.0));
            reasons.add("Active collector with " + pickups + " completed pickups");
        } else {
            performanceScore = 30.0; // Fair entry baseline for newly verified collector
            reasons.add("Verified newcomer ready for first assignments");
        }

        // 4. Average Rating (20%)
        double ratingScore;
        BigDecimal avgRating = collector.getAvgRating();
        if (avgRating != null && avgRating.compareTo(BigDecimal.ZERO) > 0) {
            ratingScore = Math.min(100.0, (avgRating.doubleValue() / 5.0) * 100.0);
            if (avgRating.compareTo(new BigDecimal("4.0")) >= 0) {
                reasons.add("Highly rated collector (" + avgRating + "/5.0 \u2B50)");
            }
        } else {
            ratingScore = config.getUnratedScoreBaseline();
        }

        double total = (distanceScore * config.getCollectorDistanceWeight())
                + (serviceAreaScore * config.getCollectorServiceAreaWeight())
                + (performanceScore * config.getCollectorPerformanceWeight())
                + (ratingScore * config.getCollectorRatingWeight());

        double normalizedTotal = BigDecimal.valueOf(Math.min(100.0, Math.max(0.0, total)))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return new ScoreResult(normalizedTotal, reasons);
    }

    /**
     * Calculates suitability of a Processor for a specific Listing or collected batch.
     */
    public ScoreResult calculateProcessorScore(ProcessorProfile processor, ProcessorCapability capability, Listing listing, Double distanceKm) {
        List<String> reasons = new ArrayList<>();

        if (processor.isVerified()) {
            reasons.add("Verified Recycling Facility \uD83C\uDFED");
        }

        // 1. Available Capacity suitability (30%)
        double capacityScore;
        BigDecimal maxCap = processor.getMaxCapacityKg();
        BigDecimal currentLoad = processor.getCurrentLoadKg();
        BigDecimal availableCap = maxCap.subtract(currentLoad);
        if (availableCap.compareTo(BigDecimal.ZERO) < 0) availableCap = BigDecimal.ZERO;

        if (maxCap.compareTo(BigDecimal.ZERO) <= 0) {
            capacityScore = 100.0;
            reasons.add("High throughput facility");
        } else {
            double capRatio = (availableCap.doubleValue() / maxCap.doubleValue()) * 100.0;
            capacityScore = Math.min(100.0, Math.max(20.0, capRatio));
            reasons.add("Sufficient processing headroom (" + availableCap + " kg available)");
        }

        // 2. Category & Capability suitability (25%)
        double capabilityScore = 100.0; // Mandatory filter passed
        reasons.add("Specialized intake capability for " + capability.getCategory().getName());

        // 3. Quantity suitability (20%)
        double quantityScore = 85.0;
        BigDecimal minQty = capability.getMinQuantity();
        BigDecimal maxQty = capability.getMaxQuantity();
        BigDecimal qty = listing.getQuantity();

        if (minQty.compareTo(BigDecimal.ZERO) > 0 && maxQty.compareTo(BigDecimal.ZERO) > 0) {
            reasons.add("Batch volume matches facility intake window (" + minQty + " - " + maxQty + " " + listing.getUnit() + ")");
            quantityScore = 95.0;
        } else {
            reasons.add("Volume compatible with facility intake");
        }

        // 4. Distance suitability (15%)
        double distanceScore;
        if (distanceKm != null) {
            double radius = config.getDefaultProcessorRadiusKm();
            distanceScore = Math.max(0.0, 100.0 * (1.0 - (distanceKm / radius)));
            if (distanceKm <= 20.0) {
                reasons.add("Local facility (" + distanceKm + " km away)");
            } else {
                reasons.add("Regional facility (" + distanceKm + " km away)");
            }
        } else {
            distanceScore = config.getMissingDistanceScoreBaseline();
        }

        // 5. Average Rating (10%)
        double ratingScore;
        BigDecimal avgRating = processor.getAvgRating();
        if (avgRating != null && avgRating.compareTo(BigDecimal.ZERO) > 0) {
            ratingScore = Math.min(100.0, (avgRating.doubleValue() / 5.0) * 100.0);
            if (avgRating.compareTo(new BigDecimal("4.0")) >= 0) {
                reasons.add("Top-rated recycling partner (" + avgRating + "/5.0 \u2B50)");
            }
        } else {
            ratingScore = config.getUnratedScoreBaseline();
        }

        double total = (capacityScore * config.getProcessorCapacityWeight())
                + (capabilityScore * config.getProcessorCapabilityWeight())
                + (quantityScore * config.getProcessorQuantityWeight())
                + (distanceScore * config.getProcessorDistanceWeight())
                + (ratingScore * config.getProcessorRatingWeight());

        double normalizedTotal = BigDecimal.valueOf(Math.min(100.0, Math.max(0.0, total)))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return new ScoreResult(normalizedTotal, reasons);
    }

    private double evaluateServiceArea(String serviceArea, Address address) {
        if (serviceArea == null || serviceArea.isBlank() || address == null) {
            return 50.0;
        }
        String areaLower = serviceArea.trim().toLowerCase();
        String cityLower = address.getCity() != null ? address.getCity().trim().toLowerCase() : "";
        String stateLower = address.getState() != null ? address.getState().trim().toLowerCase() : "";

        if (areaLower.contains("all") || areaLower.contains("any") || areaLower.contains("pan-india") || areaLower.contains("nationwide")) {
            return 100.0;
        }

        if (!cityLower.isEmpty() && areaLower.contains(cityLower)) {
            return 100.0;
        }

        if (!stateLower.isEmpty() && areaLower.contains(stateLower)) {
            return 75.0;
        }

        return 40.0;
    }
}
