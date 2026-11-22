package com.datavvy.vms.vehicle.application;

import com.datavvy.vms.vehicle.api.VehicleResponse;
import com.datavvy.vms.vehicle.api.VehicleSummary;
import com.datavvy.vms.vehicle.domain.Vehicle;

final class VehicleMapper {

    private VehicleMapper() {
    }

    static VehicleResponse toResponse(Vehicle v) {
        return new VehicleResponse(v.getId(), v.getVehicleNumber(), v.getCompany(), v.getType(), v.getBrand(),
                v.getModel(), v.getStatus(), v.getCreatedAt(), v.getUpdatedAt(), v.getVersion());
    }

    static VehicleSummary toSummary(Vehicle v) {
        return new VehicleSummary(v.getId(), v.getVehicleNumber(), v.getCompany(), v.getBrand(), v.getModel(),
                v.getStatus(), v.getUpdatedAt());
    }
}
