package com.careem.rideintel.dto;

import com.careem.rideintel.model.Ride;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;

public class RideDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRideRequest {
        @NotBlank(message = "Captain ID is required")
        private String captainId;

        @NotBlank(message = "Customer ID is required")
        private String customerId;

        @NotBlank(message = "Pickup city is required")
        private String pickupCity;

        @NotBlank(message = "Dropoff city is required")
        private String dropoffCity;

        @NotNull
        @DecimalMin(value = "0.1", message = "Distance must be positive")
        private Double distanceKm;

        @NotNull
        @DecimalMin(value = "0.0", message = "Fare cannot be negative")
        private Double fareAmount;

        @NotNull
        @Positive(message = "Duration must be positive")
        private Integer durationMinutes;

        @NotBlank(message = "Vehicle type is required")
        private String vehicleType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RideResponse {
        private Long id;
        private String captainId;
        private String customerId;
        private String pickupCity;
        private String dropoffCity;
        private Double distanceKm;
        private Double fareAmount;
        private Integer durationMinutes;
        private String status;
        private String vehicleType;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private Boolean anomalyDetected;
        private String anomalyReason;

        public static RideResponse from(Ride ride) {
            return RideResponse.builder()
                    .id(ride.getId())
                    .captainId(ride.getCaptainId())
                    .customerId(ride.getCustomerId())
                    .pickupCity(ride.getPickupCity())
                    .dropoffCity(ride.getDropoffCity())
                    .distanceKm(ride.getDistanceKm())
                    .fareAmount(ride.getFareAmount())
                    .durationMinutes(ride.getDurationMinutes())
                    .status(ride.getStatus().name())
                    .vehicleType(ride.getVehicleType())
                    .createdAt(ride.getCreatedAt())
                    .completedAt(ride.getCompletedAt())
                    .anomalyDetected(false)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsResponse {
        private String city;
        private Long totalRides;
        private Double averageFare;
        private Double averageDistanceKm;
        private Double averageDurationMinutes;
        private Long anomalyCount;
        private Double anomalyRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaptainStatsResponse {
        private String captainId;
        private Long totalRides;
        private Long completedRides;
        private Long cancelledRides;
        private Double totalEarnings;
        private Double averageRating;
        private Long anomaliesDetected;
    }
}