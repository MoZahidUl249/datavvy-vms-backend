package com.datavvy.vms.vehicle.api;

import java.time.Instant;

/** Compact row for read models (dashboard lists). */
public record VehicleSummary(Long id, String vehicleNumber, String company, String brand, String model,
        VehicleStatus status, Instant updatedAt) {
}
