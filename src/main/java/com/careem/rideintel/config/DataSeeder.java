package com.careem.rideintel.config;

import com.careem.rideintel.model.Ride;
import com.careem.rideintel.repository.RideRepository;
import com.careem.rideintel.service.AnomalyDetectionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(RideRepository rideRepository,
                               AnomalyDetectionService anomalyDetectionService) {
        return args -> {
            if (rideRepository.count() > 0) return;

            List<Ride> rides = Arrays.asList(
                    buildRide("CAP-001", "CUST-101", "Karachi",   "Karachi",   12.5, 375.0,  25, "ECONOMY"),
                    buildRide("CAP-002", "CUST-102", "Lahore",    "Lahore",    8.0,  280.0,  18, "ECONOMY"),
                    buildRide("CAP-003", "CUST-103", "Karachi",   "Karachi",   20.0, 600.0,  40, "BUSINESS"),
                    buildRide("CAP-004", "CUST-104", "Islamabad", "Islamabad", 5.5,  200.0,  12, "ECONOMY"),
                    buildRide("CAP-001", "CUST-105", "Karachi",   "Karachi",   15.0, 450.0,  30, "ECONOMY"),
                    buildRide("CAP-005", "CUST-106", "Lahore",    "Lahore",    3.0,  120.0,  10, "CARPOOL"),
                    buildRide("CAP-006", "CUST-201", "Karachi",   "Karachi",   0.5,  800.0,  5,  "ECONOMY"),
                    buildRide("CAP-007", "CUST-202", "Lahore",    "Lahore",    10.0, 2500.0, 20, "ECONOMY"),
                    buildRide("CAP-008", "CUST-203", "Karachi",   "Karachi",   50.0, 1500.0, 5,  "ECONOMY")
            );

            List<Ride> saved = rideRepository.saveAll(rides);

            for (Ride ride : saved) {
                ride.setStatus(Ride.RideStatus.COMPLETED);
                ride.setCompletedAt(LocalDateTime.now());
                rideRepository.save(ride);
                anomalyDetectionService.analyzeRide(ride);
            }

            System.out.println("[DataSeeder] Seeded " + saved.size() + " rides.");
        };
    }

    private Ride buildRide(String captainId, String customerId,
                           String from, String to,
                           double distKm, double fare,
                           int durationMin, String vehicleType) {
        return Ride.builder()
                .captainId(captainId)
                .customerId(customerId)
                .pickupCity(from)
                .dropoffCity(to)
                .distanceKm(distKm)
                .fareAmount(fare)
                .durationMinutes(durationMin)
                .vehicleType(vehicleType)
                .status(Ride.RideStatus.REQUESTED)
                .build();
    }
}