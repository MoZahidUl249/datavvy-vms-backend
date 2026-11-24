package com.datavvy.vms.driver.api;

import com.datavvy.vms.platform.time.ExpiryStatus;
import java.time.LocalDate;

/** Compact driver for the vehicle screen. */
public record DriverSummary(Long id, String fullName, String phone, String licenseNumber, LocalDate licenseExpiry,
        ExpiryStatus licenseStatus, DriverStatus status) {
}
