package com.waste2value.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceCalculatorTest {

    private DistanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DistanceCalculator();
    }

    @Test
    @DisplayName("Calculates accurate Haversine distance between Mumbai and Pune (~120 km)")
    void calculatesAccurateDistance() {
        // Mumbai (approx 19.0760, 72.8777)
        BigDecimal lat1 = new BigDecimal("19.0760");
        BigDecimal lon1 = new BigDecimal("72.8777");

        // Pune (approx 18.5204, 73.8567)
        BigDecimal lat2 = new BigDecimal("18.5204");
        BigDecimal lon2 = new BigDecimal("73.8567");

        Double distance = calculator.calculateDistanceKm(lat1, lon1, lat2, lon2);

        assertThat(distance).isNotNull();
        // Straight line distance is approximately 118-122 km
        assertThat(distance).isBetween(115.0, 125.0);
    }

    @Test
    @DisplayName("Returns null when any coordinate is missing")
    void returnsNullForMissingCoordinates() {
        assertThat(calculator.calculateDistanceKm(null, new BigDecimal("72.8777"), new BigDecimal("18.5204"), new BigDecimal("73.8567")))
                .isNull();
        assertThat(calculator.calculateDistanceKm(new BigDecimal("19.0760"), null, new BigDecimal("18.5204"), new BigDecimal("73.8567")))
                .isNull();
        assertThat(calculator.calculateDistanceKm(new BigDecimal("19.0760"), new BigDecimal("72.8777"), null, new BigDecimal("73.8567")))
                .isNull();
    }

    @Test
    @DisplayName("Treats (0.0, 0.0) coordinates as invalid/missing")
    void treatsZeroZeroAsInvalid() {
        assertThat(calculator.calculateDistanceKm(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("18.5204"), new BigDecimal("73.8567")))
                .isNull();
    }

    @Test
    @DisplayName("isWithinMaxDistance correctly evaluates distance against threshold")
    void checksWithinMaxDistance() {
        BigDecimal lat1 = new BigDecimal("19.0760");
        BigDecimal lon1 = new BigDecimal("72.8777");
        BigDecimal lat2 = new BigDecimal("18.5204");
        BigDecimal lon2 = new BigDecimal("73.8567");

        // ~120 km distance
        assertThat(calculator.isWithinMaxDistance(lat1, lon1, lat2, lon2, new BigDecimal("150.00"))).isTrue();
        assertThat(calculator.isWithinMaxDistance(lat1, lon1, lat2, lon2, new BigDecimal("100.00"))).isFalse();

        // When coordinates are missing, returns true (does not block)
        assertThat(calculator.isWithinMaxDistance(null, null, lat2, lon2, new BigDecimal("50.00"))).isTrue();
    }
}
