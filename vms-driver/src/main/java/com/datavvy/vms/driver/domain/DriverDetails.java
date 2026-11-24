package com.datavvy.vms.driver.domain;

import com.datavvy.vms.driver.api.DriverStatus;
import java.time.LocalDate;

/** Validated, canonical driver fields ready to apply. */
public record DriverDetails(String fullName, String phone, String licenseNumber, LocalDate licenseExpiry,
        String address, LocalDate joinedOn, DriverStatus status, String notes) {
}
