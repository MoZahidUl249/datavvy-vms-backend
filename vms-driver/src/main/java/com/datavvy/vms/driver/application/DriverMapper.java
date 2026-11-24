package com.datavvy.vms.driver.application;

import com.datavvy.vms.driver.api.AssignedVehicle;
import com.datavvy.vms.driver.api.AssignmentRecord;
import com.datavvy.vms.driver.api.DriverResponse;
import com.datavvy.vms.driver.api.DriverSummary;
import com.datavvy.vms.driver.domain.Driver;
import com.datavvy.vms.driver.domain.DriverAssignment;
import com.datavvy.vms.platform.time.Expiry;
import java.time.LocalDate;

final class DriverMapper {

    private DriverMapper() {
    }

    static DriverResponse toResponse(Driver d, DriverAssignment open, LocalDate today, int soonDays) {
        Expiry licence = Expiry.of(d.getLicenseExpiry(), today, soonDays);
        AssignedVehicle vehicle = open == null ? null
                : new AssignedVehicle(open.getVehicleId(), open.getVehicleNumber(), open.getCreatedAt());
        return new DriverResponse(d.getId(), d.getFullName(), d.getPhone(), d.getLicenseNumber(), d.getLicenseExpiry(),
                licence.status(), licence.daysLeft(), d.getAddress(), d.getJoinedOn(), d.getStatus(), d.getNotes(),
                vehicle, d.getCreatedAt(), d.getUpdatedAt(), d.getVersion());
    }

    static DriverSummary toSummary(Driver d, LocalDate today, int soonDays) {
        return new DriverSummary(d.getId(), d.getFullName(), d.getPhone(), d.getLicenseNumber(), d.getLicenseExpiry(),
                Expiry.of(d.getLicenseExpiry(), today, soonDays).status(), d.getStatus());
    }

    static AssignmentRecord toRecord(DriverAssignment a) {
        return new AssignmentRecord(a.getId(), a.getVehicleId(), a.getVehicleNumber(), a.getDriverId(),
                a.getDriverName(), a.getCreatedAt(), a.getCreatedBy(), a.getReleasedAt(),
                a.getReleasedAt() == null ? null : a.getUpdatedBy());
    }
}
