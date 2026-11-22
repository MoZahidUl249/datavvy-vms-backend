package com.datavvy.vms.vehicle.domain;

import com.datavvy.vms.vehicle.api.VehicleStatus;

/** Validated, canonical field values ready to apply to a {@link Vehicle}. */
public record VehicleDetails(String vehicleNumber, String company, String type, String brand, String model,
        VehicleStatus status) {
}
