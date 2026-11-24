package com.datavvy.vms.maintenance.domain;

import com.datavvy.vms.maintenance.api.MaintenanceType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Validated, tidied record fields ready to apply. */
public record MaintenanceDetails(LocalDate serviceDate, MaintenanceType type, BigDecimal cost, String vendor,
        Integer odometerKm, String notes) {
}
