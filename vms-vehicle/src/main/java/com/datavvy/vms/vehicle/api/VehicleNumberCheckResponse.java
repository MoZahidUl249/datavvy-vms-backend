package com.datavvy.vms.vehicle.api;

/** Live duplicate check for the Vehicle Number field. */
public record VehicleNumberCheckResponse(boolean valid, boolean available, String normalized,
        Long existingId, String existingVehicleNumber) {
}
