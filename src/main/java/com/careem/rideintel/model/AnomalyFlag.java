package com.careem.rideintel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_flags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Double anomalyScore;  // 0.0 to 1.0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyType type;

    @Column(nullable = false)
    private LocalDateTime flaggedAt;

    @PrePersist
    protected void onCreate() {
        flaggedAt = LocalDateTime.now();
    }

    public enum AnomalyType {
        FARE_SPIKE,        // Fare disproportionate to distance
        DURATION_MISMATCH, // Duration doesn't match distance
        GHOST_RIDE,        // Very short ride with high fare
        ROUTE_DEVIATION    // Distance unusually high for city pair
    }
}