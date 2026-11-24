package com.datavvy.vms.maintenance.application;

import com.datavvy.vms.maintenance.api.MaintenanceResponse;
import com.datavvy.vms.maintenance.domain.MaintenanceRecord;

final class MaintenanceMapper {

    private MaintenanceMapper() {
    }

    static MaintenanceResponse toResponse(MaintenanceRecord m, String vehicleNumber) {
        return new MaintenanceResponse(m.getId(), m.getVehicleId(), vehicleNumber, m.getServiceDate(), m.getType(),
                m.getCost(), m.getVendor(), m.getOdometerKm(), m.getNotes(), m.getCreatedAt(), m.getUpdatedAt(),
                m.getCreatedBy(), m.getVersion());
    }
}
