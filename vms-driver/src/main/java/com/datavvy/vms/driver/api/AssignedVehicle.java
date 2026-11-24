package com.datavvy.vms.driver.api;

import java.time.Instant;

/** The vehicle a driver currently drives. */
public record AssignedVehicle(Long vehicleId, String vehicleNumber, Instant assignedAt) {
}
