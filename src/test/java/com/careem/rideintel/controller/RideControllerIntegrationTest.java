package com.careem.rideintel.controller;

import com.careem.rideintel.dto.RideDTOs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RideControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /rides — should create a ride and return 201")
    void createRide_shouldReturn201() throws Exception {
        RideDTOs.CreateRideRequest request = RideDTOs.CreateRideRequest.builder()
                .captainId("CAP-INT-001")
                .customerId("CUST-INT-001")
                .pickupCity("Karachi")
                .dropoffCity("Karachi")
                .distanceKm(12.0)
                .fareAmount(360.0)
                .durationMinutes(25)
                .vehicleType("ECONOMY")
                .build();

        mockMvc.perform(post("/api/v1/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.captainId").value("CAP-INT-001"))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("POST /rides — missing required fields should return 400")
    void createRide_missingFields_shouldReturn400() throws Exception {
        RideDTOs.CreateRideRequest request = RideDTOs.CreateRideRequest.builder()
                .captainId("CAP-INT-002")
                // Missing customerId, pickupCity, etc.
                .build();

        mockMvc.perform(post("/api/v1/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Full ride lifecycle — create, complete, check anomaly detection")
    void rideLifecycle_withAnomalousData_shouldFlagAnomaly() throws Exception {
        // Create a ghost ride
        RideDTOs.CreateRideRequest request = RideDTOs.CreateRideRequest.builder()
                .captainId("CAP-INT-003")
                .customerId("CUST-INT-003")
                .pickupCity("Lahore")
                .dropoffCity("Lahore")
                .distanceKm(0.3)
                .fareAmount(950.0)
                .durationMinutes(3)
                .vehicleType("ECONOMY")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        RideDTOs.RideResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), RideDTOs.RideResponse.class);

        // Complete it — anomaly detection fires here
        mockMvc.perform(patch("/api/v1/rides/" + created.getId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomalyDetected").value(true));
    }

    @Test
    @DisplayName("GET /rides/{id} — non-existent ride should return 404")
    void getRide_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/rides/99999"))
                .andExpect(status().isNotFound());
    }
}