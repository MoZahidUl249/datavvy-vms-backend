package com.datavvy.vms.maintenance.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Header of a vehicle's maintenance history. {@code last*} are null when there are no records. */
public record MaintenanceHistorySummary(Long vehicleId, long recordCount, BigDecimal totalCost,
        BigDecimal costLast12Months, LocalDate lastServiceDate, MaintenanceType lastServiceType,
        Integer lastOdometerKm) {
}
