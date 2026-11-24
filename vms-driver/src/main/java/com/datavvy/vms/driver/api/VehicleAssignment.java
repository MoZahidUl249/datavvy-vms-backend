package com.datavvy.vms.driver.api;

import java.time.Instant;

/** A vehicle's current driver; {@code driver} and the rest are null when nobody is assigned. */
public record VehicleAssignment(Long vehicleId, DriverSummary driver, Instant assignedAt, String assignedBy) {
}
