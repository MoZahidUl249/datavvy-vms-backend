package com.datavvy.vms.driver.application;

import com.datavvy.vms.vehicle.api.VehicleDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** A deleted vehicle frees its driver; the assignment history stays. */
@Component
class VehicleAssignmentCleanup {

    private final AssignmentService assignments;

    VehicleAssignmentCleanup(AssignmentService assignments) {
        this.assignments = assignments;
    }

    @EventListener
    public void on(VehicleDeletedEvent event) {
        assignments.releaseForDeletedVehicle(event.vehicleId());
    }
}
