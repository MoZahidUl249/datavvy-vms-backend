package com.datavvy.vms.maintenance.application;

import com.datavvy.vms.maintenance.domain.MaintenanceRecordRepository;
import com.datavvy.vms.vehicle.api.VehicleDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** A deleted vehicle takes its maintenance history with it (same transaction as the vehicle delete). */
@Component
class VehicleMaintenanceCleanup {

    private final MaintenanceRecordRepository records;

    VehicleMaintenanceCleanup(MaintenanceRecordRepository records) {
        this.records = records;
    }

    @EventListener
    @Transactional
    public void on(VehicleDeletedEvent event) {
        records.deleteByVehicle(event.vehicleId());
    }
}
