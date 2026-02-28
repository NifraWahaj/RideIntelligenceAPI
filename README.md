# Ride Intelligence API

A production-grade Spring Boot REST API for ride-hailing analytics and real-time anomaly detection. Designed to mirror the backend architecture used by platforms like Careem — microservice-ready, clean layered architecture, fully tested.

---

## What It Does

This service handles the core **ride lifecycle** (create → in-progress → complete/cancel) and runs an **anomaly detection engine** automatically when a ride is completed. The engine flags suspicious ride patterns that are common in ride-hailing fraud scenarios.

### Anomaly Detection

The detection engine evaluates 4 rule types on every completed ride:

| Type | Description | Example |
|------|-------------|---------|
| `FARE_SPIKE` | Fare-per-km ratio exceeds 150 PKR/km | 10km ride charged 2,500 PKR |
| `GHOST_RIDE` | Sub-1km trip with fare above 500 PKR | 0.3km ride charged 900 PKR |
| `DURATION_MISMATCH` | Implied speed is impossible or near-zero | 50km in 5 min = 600 km/h |
| `ROUTE_DEVIATION` | Reserved for future city-pair baseline checks | — |

Each flag gets a **normalized anomaly score (0.0–1.0)** based on how far the ride deviates from the threshold. Multiple flags on one ride produce a composite score.

---

## Tech Stack

- **Java 8** + **Spring Boot 2.7**
- **Spring Data JPA** with JPQL custom queries
- **H2** in-memory DB (dev) / **PostgreSQL** (production)
- **Lombok** for clean model code
- **Springdoc/Swagger UI** for auto-generated API docs
- **JUnit 5 + Mockito** for unit and integration tests

---

## Getting Started

### Prerequisites
- Java 8+
- Maven 3.6+

### Run Locally

```bash
git clone https://github.com/NifraWahaj/ride-intelligence-api
cd ride-intelligence-api
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

On startup, **demo data is automatically seeded** — including intentionally anomalous rides so you can see the detection engine in action immediately.

### Explore the API

| Interface | URL |
|-----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |
| Raw API Docs | http://localhost:8080/api-docs |

---

## API Reference

### Rides

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/rides` | Create a new ride |
| `GET` | `/api/v1/rides/{id}` | Get ride by ID |
| `PATCH` | `/api/v1/rides/{id}/complete` | Complete ride — triggers anomaly detection |
| `PATCH` | `/api/v1/rides/{id}/cancel` | Cancel a ride |
| `GET` | `/api/v1/rides/captain/{captainId}` | All rides for a captain |
| `GET` | `/api/v1/rides/customer/{customerId}` | All rides for a customer |

### Analytics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/analytics/cities` | City-level stats + anomaly rates |
| `GET` | `/api/v1/analytics/captains/{captainId}` | Captain performance + earnings |

### Create Ride — Example Request

```json
POST /api/v1/rides
{
  "captainId": "CAP-001",
  "customerId": "CUST-101",
  "pickupCity": "Karachi",
  "dropoffCity": "Karachi",
  "distanceKm": 12.5,
  "fareAmount": 375.0,
  "durationMinutes": 25,
  "vehicleType": "ECONOMY"
}
```

### Complete Ride — Example Response (with anomaly flag)

```json
PATCH /api/v1/rides/7/complete
{
  "id": 7,
  "captainId": "CAP-006",
  "distanceKm": 0.5,
  "fareAmount": 800.0,
  "status": "COMPLETED",
  "anomalyDetected": true,
  "anomalyReason": "Ghost ride suspected: 0.50 km traveled but fare charged 800.0 PKR"
}
```

---

## Project Structure

```
src/
├── main/java/com/careem/rideintel/
│   ├── controller/        # REST controllers (Ride, Analytics)
│   ├── service/           # Business logic + AnomalyDetectionService
│   ├── repository/        # Spring Data JPA repos with custom JPQL
│   ├── model/             # JPA entities (Ride, AnomalyFlag)
│   ├── dto/               # Request/Response DTOs
│   ├── exception/         # GlobalExceptionHandler + custom exceptions
│   └── config/            # DataSeeder for demo data
└── test/
    ├── service/           # Unit tests (AnomalyDetectionServiceTest)
    └── controller/        # Integration tests (RideControllerIntegrationTest)
```

---

## Running Tests

```bash
mvn test
```

Unit tests cover all 4 anomaly detection rules, score normalization, and clean ride cases. Integration tests cover the full ride lifecycle including anomaly detection via MockMvc.


---

## Future Improvements

- JWT authentication for captain/customer endpoints
- Configurable thresholds via `application.properties` instead of hardcoded constants
- Sliding-window statistics for dynamic threshold calculation per city
- Kafka integration for async anomaly event publishing
- Docker + docker-compose for one-command containerized setup