package com.datavvy.vms.vehicle.api;

import java.time.Instant;

/** What clients see. Never the JPA entity. */
public record VehicleResponse(
        Long id,
        String vehicleNumber,
        String company,
        String type,
        String brand,
        String model,
        VehicleStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
