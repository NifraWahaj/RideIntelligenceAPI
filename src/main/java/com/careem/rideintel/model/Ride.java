package com.careem.rideintel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String captainId;

    @NotBlank
    @Column(nullable = false)
    private String customerId;

    @NotBlank
    @Column(nullable = false)
    private String pickupCity;

    @NotBlank
    @Column(nullable = false)
    private String dropoffCity;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false)
    private Double distanceKm;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false)
    private Double fareAmount;

    @NotNull
    @Column(nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private String vehicleType;  // ECONOMY, BUSINESS, CARPOOL

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RideStatus {
        REQUESTED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}