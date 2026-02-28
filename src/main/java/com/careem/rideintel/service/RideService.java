package com.careem.rideintel.service;

import com.careem.rideintel.dto.RideDTOs;
import com.careem.rideintel.exception.ResourceNotFoundException;
import com.careem.rideintel.model.AnomalyFlag;
import com.careem.rideintel.model.Ride;
import com.careem.rideintel.repository.AnomalyFlagRepository;
import com.careem.rideintel.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final AnomalyDetectionService anomalyDetectionService;

    public RideService(RideRepository rideRepository,
                       AnomalyFlagRepository anomalyFlagRepository,
                       AnomalyDetectionService anomalyDetectionService) {
        this.rideRepository          = rideRepository;
        this.anomalyFlagRepository   = anomalyFlagRepository;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @Transactional
    public RideDTOs.RideResponse createRide(RideDTOs.CreateRideRequest request) {
        Ride ride = Ride.builder()
                .captainId(request.getCaptainId())
                .customerId(request.getCustomerId())
                .pickupCity(request.getPickupCity())
                .dropoffCity(request.getDropoffCity())
                .distanceKm(request.getDistanceKm())
                .fareAmount(request.getFareAmount())
                .durationMinutes(request.getDurationMinutes())
                .vehicleType(request.getVehicleType())
                .status(Ride.RideStatus.REQUESTED)
                .build();

        Ride saved = rideRepository.save(ride);
        return RideDTOs.RideResponse.from(saved);
    }

    @Transactional
    public RideDTOs.RideResponse completeRide(Long rideId) {
        Ride ride = findRideOrThrow(rideId);

        ride.setStatus(Ride.RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        rideRepository.save(ride);

        // Run anomaly detection on completion
        Optional<AnomalyFlag> flag = anomalyDetectionService.analyzeRide(ride);

        RideDTOs.RideResponse response = RideDTOs.RideResponse.from(ride);
        flag.ifPresent(f -> {
            response.setAnomalyDetected(true);
            response.setAnomalyReason(f.getReason());
        });

        return response;
    }

    @Transactional
    public RideDTOs.RideResponse cancelRide(Long rideId) {
        Ride ride = findRideOrThrow(rideId);
        ride.setStatus(Ride.RideStatus.CANCELLED);
        rideRepository.save(ride);
        return RideDTOs.RideResponse.from(ride);
    }

    public RideDTOs.RideResponse getRideById(Long rideId) {
        Ride ride = findRideOrThrow(rideId);
        RideDTOs.RideResponse response = RideDTOs.RideResponse.from(ride);

        anomalyFlagRepository.findByRideId(rideId).ifPresent(flag -> {
            response.setAnomalyDetected(true);
            response.setAnomalyReason(flag.getReason());
        });

        return response;
    }

    public List<RideDTOs.RideResponse> getRidesByCaptain(String captainId) {
        return rideRepository.findByCaptainId(captainId)
                .stream()
                .map(RideDTOs.RideResponse::from)
                .collect(Collectors.toList());
    }

    public List<RideDTOs.RideResponse> getRidesByCustomer(String customerId) {
        return rideRepository.findByCustomerId(customerId)
                .stream()
                .map(RideDTOs.RideResponse::from)
                .collect(Collectors.toList());
    }

    public List<RideDTOs.AnalyticsResponse> getCityAnalytics() {
        List<Object[]> raw = rideRepository.getCityAnalytics();
        return raw.stream().map(row -> {
            String city        = (String) row[0];
            Long totalRides    = (Long) row[1];
            Double avgFare     = (Double) row[2];
            Double avgDistance = (Double) row[3];
            Double avgDuration = (Double) row[4];
            Long anomalyCount  = anomalyFlagRepository.countByCity(city);
            double anomalyRate = totalRides > 0 ? (double) anomalyCount / totalRides : 0.0;

            return RideDTOs.AnalyticsResponse.builder()
                    .city(city)
                    .totalRides(totalRides)
                    .averageFare(avgFare)
                    .averageDistanceKm(avgDistance)
                    .averageDurationMinutes(avgDuration)
                    .anomalyCount(anomalyCount)
                    .anomalyRate(anomalyRate)
                    .build();
        }).collect(Collectors.toList());
    }

    public RideDTOs.CaptainStatsResponse getCaptainStats(String captainId) {
        List<Ride> rides       = rideRepository.findByCaptainId(captainId);
        long completed         = rides.stream().filter(r -> r.getStatus() == Ride.RideStatus.COMPLETED).count();
        long cancelled         = rides.stream().filter(r -> r.getStatus() == Ride.RideStatus.CANCELLED).count();
        double totalEarnings   = rides.stream()
                .filter(r -> r.getStatus() == Ride.RideStatus.COMPLETED)
                .mapToDouble(Ride::getFareAmount)
                .sum();
        Long anomalies         = anomalyFlagRepository.countByCaptainId(captainId);

        return RideDTOs.CaptainStatsResponse.builder()
                .captainId(captainId)
                .totalRides((long) rides.size())
                .completedRides(completed)
                .cancelledRides(cancelled)
                .totalEarnings(totalEarnings)
                .anomaliesDetected(anomalies)
                .build();
    }

    private Ride findRideOrThrow(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with id: " + rideId));
    }
}