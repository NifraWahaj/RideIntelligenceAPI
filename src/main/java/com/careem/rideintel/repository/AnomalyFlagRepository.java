package com.careem.rideintel.repository;

import com.careem.rideintel.model.AnomalyFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnomalyFlagRepository extends JpaRepository<AnomalyFlag, Long> {

    Optional<AnomalyFlag> findByRideId(Long rideId);

    List<AnomalyFlag> findByType(AnomalyFlag.AnomalyType type);

    @Query("SELECT COUNT(a) FROM AnomalyFlag a WHERE a.ride.captainId = :captainId")
    Long countByCaptainId(@Param("captainId") String captainId);

    @Query("SELECT COUNT(a) FROM AnomalyFlag a JOIN a.ride r WHERE r.pickupCity = :city")
    Long countByCity(@Param("city") String city);

    @Query("SELECT a FROM AnomalyFlag a WHERE a.anomalyScore >= :minScore ORDER BY a.anomalyScore DESC")
    List<AnomalyFlag> findHighScoreAnomalies(@Param("minScore") Double minScore);
}