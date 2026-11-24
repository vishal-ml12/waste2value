package com.waste2value.matching;

import com.waste2value.address.Address;
import com.waste2value.category.Category;
import com.waste2value.collector.CollectorProfile;
import com.waste2value.listing.Listing;
import com.waste2value.processor.ProcessorCapability;
import com.waste2value.processor.ProcessorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingScoreCalculatorTest {

    private MatchingConfiguration config;
    private MatchingScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        config = new MatchingConfiguration();
        calculator = new MatchingScoreCalculator(config);
    }

    @Test
    @DisplayName("Collector scoring: perfect candidate with close distance, direct city match, high pickups, and top rating achieves > 90 score")
    void testPerfectCollectorScoring() {
        Address listingAddress = new Address();
        listingAddress.setCity("Mumbai");
        listingAddress.setState("Maharashtra");

        Listing listing = new Listing();
        listing.setAddress(listingAddress);

        CollectorProfile collector = new CollectorProfile();
        collector.setVerified(true);
        collector.setServiceArea("Mumbai Metropolitan Region");
        collector.setMaxDistanceKm(new BigDecimal("50.00"));
        collector.setTotalPickups(25); // >= benchmark 20
        collector.setAvgRating(new BigDecimal("5.00"));

        Double distanceKm = 2.5; // Very close: (1 - 2.5/50) = 95% distance score

        MatchingScoreCalculator.ScoreResult result = calculator.calculateCollectorScore(collector, listing, distanceKm);

        assertThat(result.totalScore()).isGreaterThanOrEqualTo(90.0);
        assertThat(result.reasons()).anyMatch(r -> r.contains("Close proximity"));
        assertThat(result.reasons()).anyMatch(r -> r.contains("Direct service area match"));
        assertThat(result.reasons()).anyMatch(r -> r.contains("completed pickups"));
        assertThat(result.reasons()).anyMatch(r -> r.contains("Highly rated"));
    }

    @Test
    @DisplayName("Collector scoring: handles missing coordinates and unrated newcomer safely without NaN or crash")
    void testCollectorMissingDistanceAndRating() {
        Address listingAddress = new Address();
        listingAddress.setCity("Delhi");

        Listing listing = new Listing();
        listing.setAddress(listingAddress);

        CollectorProfile collector = new CollectorProfile();
        collector.setVerified(true);
        collector.setServiceArea("Delhi NCR");
        collector.setMaxDistanceKm(new BigDecimal("40.00"));
        collector.setTotalPickups(0); // newcomer
        collector.setAvgRating(BigDecimal.ZERO); // unrated

        MatchingScoreCalculator.ScoreResult result = calculator.calculateCollectorScore(collector, listing, null);

        assertThat(result.totalScore()).isBetween(40.0, 75.0);
        assertThat(result.reasons()).anyMatch(r -> r.contains("Verified newcomer"));
    }

    @Test
    @DisplayName("Processor scoring: top facility with high available capacity, matching quantity, and top rating achieves high score")
    void testTopProcessorScoring() {
        Category category = new Category();
        category.setName("HDPE Plastics");

        Listing listing = new Listing();
        listing.setQuantity(new BigDecimal("500.00"));
        listing.setUnit("kg");

        ProcessorProfile processor = new ProcessorProfile();
        processor.setVerified(true);
        processor.setMaxCapacityKg(new BigDecimal("10000.00"));
        processor.setCurrentLoadKg(new BigDecimal("2000.00")); // 8000 kg available (80%)
        processor.setAvgRating(new BigDecimal("4.80"));

        ProcessorCapability capability = new ProcessorCapability();
        capability.setProcessor(processor);
        capability.setCategory(category);
        capability.setMinQuantity(new BigDecimal("100.00"));
        capability.setMaxQuantity(new BigDecimal("1000.00"));

        Double distanceKm = 15.0;

        MatchingScoreCalculator.ScoreResult result = calculator.calculateProcessorScore(processor, capability, listing, distanceKm);

        assertThat(result.totalScore()).isGreaterThanOrEqualTo(80.0);
        assertThat(result.reasons()).anyMatch(r -> r.contains("Specialized intake capability"));
        assertThat(result.reasons()).anyMatch(r -> r.contains("Sufficient processing headroom"));
    }

    @Test
    @DisplayName("Processor scoring: handles missing distance and unrated facility gracefully")
    void testProcessorMissingDistanceAndUnrated() {
        Category category = new Category();
        category.setName("PET Bottles");

        Listing listing = new Listing();
        listing.setQuantity(new BigDecimal("250.00"));
        listing.setUnit("kg");

        ProcessorProfile processor = new ProcessorProfile();
        processor.setVerified(true);
        processor.setMaxCapacityKg(new BigDecimal("5000.00"));
        processor.setCurrentLoadKg(new BigDecimal("1000.00"));
        processor.setAvgRating(BigDecimal.ZERO);

        ProcessorCapability capability = new ProcessorCapability();
        capability.setProcessor(processor);
        capability.setCategory(category);
        capability.setMinQuantity(BigDecimal.ZERO);
        capability.setMaxQuantity(BigDecimal.ZERO);

        MatchingScoreCalculator.ScoreResult result = calculator.calculateProcessorScore(processor, capability, listing, null);

        assertThat(result.totalScore()).isBetween(50.0, 85.0);
    }
}
