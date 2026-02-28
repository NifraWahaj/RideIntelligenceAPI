package com.careem.rideintel.controller;

import com.careem.rideintel.dto.RideDTOs;
import com.careem.rideintel.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Rides", description = "Ride lifecycle management")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    @Operation(summary = "Create a new ride request")
    public ResponseEntity<RideDTOs.RideResponse> createRide(
            @Valid @RequestBody RideDTOs.CreateRideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.createRide(request));
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Get ride details by ID")
    public ResponseEntity<RideDTOs.RideResponse> getRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.getRideById(rideId));
    }

    @PatchMapping("/{rideId}/complete")
    @Operation(summary = "Mark ride as completed — triggers anomaly detection")
    public ResponseEntity<RideDTOs.RideResponse> completeRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.completeRide(rideId));
    }

    @PatchMapping("/{rideId}/cancel")
    @Operation(summary = "Cancel a ride")
    public ResponseEntity<RideDTOs.RideResponse> cancelRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.cancelRide(rideId));
    }

    @GetMapping("/captain/{captainId}")
    @Operation(summary = "Get all rides for a captain")
    public ResponseEntity<List<RideDTOs.RideResponse>> getRidesByCaptain(
            @PathVariable String captainId) {
        return ResponseEntity.ok(rideService.getRidesByCaptain(captainId));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all rides for a customer")
    public ResponseEntity<List<RideDTOs.RideResponse>> getRidesByCustomer(
            @PathVariable String customerId) {
        return ResponseEntity.ok(rideService.getRidesByCustomer(customerId));
    }
}