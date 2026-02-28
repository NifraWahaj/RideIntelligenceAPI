package com.careem.rideintel.controller;

import com.careem.rideintel.dto.RideDTOs;
import com.careem.rideintel.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Ride intelligence and statistics")
public class AnalyticsController {

    private final RideService rideService;

    public AnalyticsController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/cities")
    @Operation(summary = "City-level aggregated ride stats with anomaly rates")
    public ResponseEntity<List<RideDTOs.AnalyticsResponse>> getCityAnalytics() {
        return ResponseEntity.ok(rideService.getCityAnalytics());
    }

    @GetMapping("/captains/{captainId}")
    @Operation(summary = "Performance stats for a specific captain")
    public ResponseEntity<RideDTOs.CaptainStatsResponse> getCaptainStats(
            @PathVariable String captainId) {
        return ResponseEntity.ok(rideService.getCaptainStats(captainId));
    }
}