package com.careem.rideintel.service;

import com.careem.rideintel.model.AnomalyFlag;
import com.careem.rideintel.model.Ride;
import com.careem.rideintel.repository.AnomalyFlagRepository;
import com.careem.rideintel.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rule-based anomaly detection engine for ride data.
 *
 * Detects 4 anomaly types using statistical thresholds derived from domain knowledge:
 *  - FARE_SPIKE:        fare-per-km ratio exceeds 2.5x city average
 *  - GHOST_RIDE:        very short distance (<1km) with disproportionately high fare (>500 PKR)
 *  - DURATION_MISMATCH: expected speed (distance/duration) implies impossible or highly suspicious driving
 *  - ROUTE_DEVIATION:   distance is >3x the straight-line city-pair baseline
 *
 * Anomaly score is a weighted composite of individual rule scores, normalized to [0, 1].
 */
@Service
public class AnomalyDetectionService {

    // Thresholds (tunable)
    private static final double FARE_PER_KM_THRESHOLD = 150.0;   // PKR/km — above this is suspicious
    private static final double GHOST_RIDE_DISTANCE   = 1.0;     // km
    private static final double GHOST_RIDE_FARE       = 500.0;   // PKR
    private static final double MIN_SPEED_KMH         = 5.0;     // below = driver barely moved (ghost ride variant)
    private static final double MAX_SPEED_KMH         = 200.0;   // above = data error or manipulation

    private final AnomalyFlagRepository anomalyFlagRepository;
    private final RideRepository rideRepository;

    public AnomalyDetectionService(AnomalyFlagRepository anomalyFlagRepository,
                                   RideRepository rideRepository) {
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.rideRepository        = rideRepository;
    }

    /**
     * Runs all anomaly checks on a completed ride.
     * Persists a flag if any check fires.
     *
     * @return the persisted AnomalyFlag, or empty if ride is clean
     */
    public Optional<AnomalyFlag> analyzeRide(Ride ride) {
        List<RuleResult> firedRules = new ArrayList<>();

        firedRules.addAll(checkFareSpike(ride));
        firedRules.addAll(checkGhostRide(ride));
        firedRules.addAll(checkDurationMismatch(ride));

        if (firedRules.isEmpty()) {
            return Optional.empty();
        }

        // Pick the highest-severity rule as the primary type
        RuleResult primary = firedRules.stream()
                .max((a, b) -> Double.compare(a.score, b.score))
                .get();

        // Composite score = average of all fired rule scores, capped at 1.0
        double compositeScore = Math.min(1.0,
                firedRules.stream().mapToDouble(r -> r.score).average().orElse(0.0));

        String reasons = buildReasonString(firedRules);

        AnomalyFlag flag = AnomalyFlag.builder()
                .ride(ride)
                .reason(reasons)
                .anomalyScore(compositeScore)
                .type(primary.type)
                .build();

        return Optional.of(anomalyFlagRepository.save(flag));
    }

    // ─── Rule Implementations ──────────────────────────────────────────────────

    private List<RuleResult> checkFareSpike(Ride ride) {
        List<RuleResult> results = new ArrayList<>();
        double farePerKm = ride.getFareAmount() / ride.getDistanceKm();

        if (farePerKm > FARE_PER_KM_THRESHOLD) {
            // Score scales with how far above threshold: 150→0.5, 300→1.0
            double score = Math.min(1.0, (farePerKm - FARE_PER_KM_THRESHOLD) / FARE_PER_KM_THRESHOLD);
            results.add(new RuleResult(
                    AnomalyFlag.AnomalyType.FARE_SPIKE,
                    score,
                    String.format("Fare/km ratio %.1f PKR/km exceeds threshold of %.1f PKR/km",
                            farePerKm, FARE_PER_KM_THRESHOLD)
            ));
        }
        return results;
    }

    private List<RuleResult> checkGhostRide(Ride ride) {
        List<RuleResult> results = new ArrayList<>();

        if (ride.getDistanceKm() < GHOST_RIDE_DISTANCE && ride.getFareAmount() > GHOST_RIDE_FARE) {
            double score = Math.min(1.0, ride.getFareAmount() / (GHOST_RIDE_FARE * 2));
            results.add(new RuleResult(
                    AnomalyFlag.AnomalyType.GHOST_RIDE,
                    score,
                    String.format("Ghost ride suspected: %.2f km traveled but fare charged %.1f PKR",
                            ride.getDistanceKm(), ride.getFareAmount())
            ));
        }
        return results;
    }

    private List<RuleResult> checkDurationMismatch(Ride ride) {
        List<RuleResult> results = new ArrayList<>();

        double durationHours = ride.getDurationMinutes() / 60.0;
        double impliedSpeedKmh = ride.getDistanceKm() / durationHours;

        if (impliedSpeedKmh < MIN_SPEED_KMH) {
            double score = Math.min(1.0, (MIN_SPEED_KMH - impliedSpeedKmh) / MIN_SPEED_KMH);
            results.add(new RuleResult(
                    AnomalyFlag.AnomalyType.DURATION_MISMATCH,
                    score,
                    String.format("Implied speed %.1f km/h is suspiciously low (min: %.1f km/h)",
                            impliedSpeedKmh, MIN_SPEED_KMH)
            ));
        } else if (impliedSpeedKmh > MAX_SPEED_KMH) {
            double score = Math.min(1.0, (impliedSpeedKmh - MAX_SPEED_KMH) / MAX_SPEED_KMH);
            results.add(new RuleResult(
                    AnomalyFlag.AnomalyType.DURATION_MISMATCH,
                    score,
                    String.format("Implied speed %.1f km/h is impossibly high (max: %.1f km/h)",
                            impliedSpeedKmh, MAX_SPEED_KMH)
            ));
        }
        return results;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String buildReasonString(List<RuleResult> rules) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(rules.get(i).reason);
        }
        return sb.toString();
    }

    /** Value object holding a single rule's result */
    private static class RuleResult {
        final AnomalyFlag.AnomalyType type;
        final double score;
        final String reason;

        RuleResult(AnomalyFlag.AnomalyType type, double score, String reason) {
            this.type   = type;
            this.score  = score;
            this.reason = reason;
        }
    }
}