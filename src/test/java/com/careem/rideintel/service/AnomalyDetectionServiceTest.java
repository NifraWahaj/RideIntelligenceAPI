package com.careem.rideintel.service;

import com.careem.rideintel.model.AnomalyFlag;
import com.careem.rideintel.model.Ride;
import com.careem.rideintel.repository.AnomalyFlagRepository;
import com.careem.rideintel.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private AnomalyFlagRepository anomalyFlagRepository;

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    @BeforeEach
    void setUp() {
        when(anomalyFlagRepository.save(any(AnomalyFlag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Clean ride should not be flagged")
    void cleanRide_shouldNotFlag() {
        Ride ride = buildRide(15.0, 450.0, 30);  // 30 PKR/km, 30 km/h — normal

        Optional<AnomalyFlag> result = anomalyDetectionService.analyzeRide(ride);

        assertFalse(result.isPresent(), "Normal ride should not produce anomaly flag");
    }

    @Test
    @DisplayName("Ghost ride should be detected — short distance, high fare")
    void ghostRide_shouldBeFlagged() {
        Ride ride = buildRide(0.3, 900.0, 5);  // 0.3km, 900 PKR — obvious ghost ride

        Optional<AnomalyFlag> result = anomalyDetectionService.analyzeRide(ride);

        assertTrue(result.isPresent(), "Ghost ride should produce anomaly flag");
        assertEquals(AnomalyFlag.AnomalyType.GHOST_RIDE, result.get().getType());
        assertTrue(result.get().getAnomalyScore() > 0.0);
    }

    @Test
    @DisplayName("Fare spike should be detected — fare/km ratio too high")
    void fareSpikeRide_shouldBeFlagged() {
        Ride ride = buildRide(10.0, 2500.0, 20);  // 250 PKR/km — way above threshold

        Optional<AnomalyFlag> result = anomalyDetectionService.analyzeRide(ride);

        assertTrue(result.isPresent(), "Fare spike should produce anomaly flag");
        assertEquals(AnomalyFlag.AnomalyType.FARE_SPIKE, result.get().getType());
    }

    @Test
    @DisplayName("Duration mismatch — impossibly high speed")
    void impossibleSpeed_shouldBeFlagged() {
        Ride ride = buildRide(50.0, 1500.0, 5);  // 50km in 5 min = 600 km/h

        Optional<AnomalyFlag> result = anomalyDetectionService.analyzeRide(ride);

        assertTrue(result.isPresent());
        assertEquals(AnomalyFlag.AnomalyType.DURATION_MISMATCH, result.get().getType());
    }

    @Test
    @DisplayName("Duration mismatch — suspiciously slow speed")
    void suspiciouslySlowSpeed_shouldBeFlagged() {
        Ride ride = buildRide(1.0, 100.0, 120);  // 1km in 2 hours = 0.5 km/h

        Optional<AnomalyFlag> result = anomalyDetectionService.analyzeRide(ride);

        assertTrue(result.isPresent());
        assertEquals(AnomalyFlag.AnomalyType.DURATION_MISMATCH, result.get().getType());
    }

    @Test
    @DisplayName("Anomaly score should be between 0.0 and 1.0")
    void anomalyScore_shouldBeNormalized() {
        Ride ride = buildRide(0.1, 9999.0, 2);  // Extreme values

        Optional<AnomalyFlag> result = anomalyDetectionService.analyzeRide(ride);

        assertTrue(result.isPresent());
        assertTrue(result.get().getAnomalyScore() >= 0.0);
        assertTrue(result.get().getAnomalyScore() <= 1.0);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Ride buildRide(double distanceKm, double fareAmount, int durationMinutes) {
        Ride ride = new Ride();
        ride.setId(1L);
        ride.setCaptainId("CAP-TEST");
        ride.setCustomerId("CUST-TEST");
        ride.setPickupCity("Karachi");
        ride.setDropoffCity("Karachi");
        ride.setDistanceKm(distanceKm);
        ride.setFareAmount(fareAmount);
        ride.setDurationMinutes(durationMinutes);
        ride.setVehicleType("ECONOMY");
        ride.setStatus(Ride.RideStatus.COMPLETED);
        return ride;
    }
}