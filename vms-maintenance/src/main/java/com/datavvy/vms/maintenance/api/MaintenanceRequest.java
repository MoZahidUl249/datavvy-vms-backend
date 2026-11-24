package com.datavvy.vms.maintenance.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Create / update a maintenance record. {@code cost} is in BDT. {@code version} is required on update. */
public record MaintenanceRequest(
        @NotNull(message = "Enter the service date, like 2026-11-20.")
        LocalDate serviceDate,

        @NotNull(message = "Choose what was done, like ROUTINE_SERVICE.")
        MaintenanceType type,

        @NotNull(message = "Enter the cost in taka, like 4500. Use 0 for free work.")
        @DecimalMin(value = "0.00", message = "The cost can't be negative.")
        @Digits(integer = 10, fraction = 2, message = "Enter the cost with at most 2 decimals, like 4500.50.")
        BigDecimal cost,

        @NotBlank(message = "Enter the workshop or vendor, like Navana Service Centre.")
        @Size(max = 120, message = "Keep the workshop name under 120 characters.")
        String vendor,

        @PositiveOrZero(message = "The odometer reading can't be negative.")
        Integer odometerKm,

        @Size(max = 1000, message = "Keep the notes under 1000 characters.")
        String notes,

        Long version) {
}
