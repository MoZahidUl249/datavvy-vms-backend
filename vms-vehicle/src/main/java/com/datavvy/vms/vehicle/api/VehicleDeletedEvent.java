package com.datavvy.vms.vehicle.api;

/**
 * Published inside the delete transaction after a vehicle is removed. Modules
 * that keep records by vehicle id (documents, maintenance, driver assignments)
 * listen for it and clean up their own tables — the vehicle module never
 * touches them (ADR-0003, ADR-0007).
 */
public record VehicleDeletedEvent(Long vehicleId, String vehicleNumber) {
}
