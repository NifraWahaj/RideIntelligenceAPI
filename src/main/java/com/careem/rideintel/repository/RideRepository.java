package com.careem.rideintel.repository;

import com.careem.rideintel.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByCaptainId(String captainId);

    List<Ride> findByCustomerId(String customerId);

    List<Ride> findByPickupCity(String city);

    List<Ride> findByStatus(Ride.RideStatus status);

    // Average fare per city
    @Query("SELECT r.pickupCity, AVG(r.fareAmount) FROM Ride r WHERE r.status = 'COMPLETED' GROUP BY r.pickupCity")
    List<Object[]> getAverageFareByCity();

    // Rides in a date range
    @Query("SELECT r FROM Ride r WHERE r.createdAt BETWEEN :start AND :end")
    List<Ride> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Total earnings per captain
    @Query("SELECT r.captainId, SUM(r.fareAmount), COUNT(r) FROM Ride r WHERE r.status = 'COMPLETED' GROUP BY r.captainId")
    List<Object[]> getCaptainEarnings();

    // City-level ride counts
    @Query("SELECT r.pickupCity, COUNT(r), AVG(r.fareAmount), AVG(r.distanceKm), AVG(r.durationMinutes) " +
            "FROM Ride r WHERE r.status = 'COMPLETED' GROUP BY r.pickupCity")
    List<Object[]> getCityAnalytics();

    // High fare rides (potential anomalies - fare/distance ratio outliers)
    @Query("SELECT r FROM Ride r WHERE (r.fareAmount / r.distanceKm) > :threshold AND r.status = 'COMPLETED'")
    List<Ride> findHighFareRatioRides(@Param("threshold") Double threshold);

    // Completed rides count per captain
    @Query("SELECT COUNT(r) FROM Ride r WHERE r.captainId = :captainId AND r.status = 'COMPLETED'")
    Long countCompletedRidesByCaptain(@Param("captainId") String captainId);
}