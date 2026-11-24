package com.datavvy.vms.driver.api;

import com.datavvy.vms.platform.time.ExpiryStatus;
import java.time.Instant;
import java.time.LocalDate;

/** A driver with licence state for today and the current vehicle (null when unassigned). */
public record DriverResponse(
        Long id,
        String fullName,
        String phone,
        String licenseNumber,
        LocalDate licenseExpiry,
        ExpiryStatus licenseStatus,
        Integer licenseDaysLeft,
        String address,
        LocalDate joinedOn,
        DriverStatus status,
        String notes,
        AssignedVehicle assignedVehicle,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
