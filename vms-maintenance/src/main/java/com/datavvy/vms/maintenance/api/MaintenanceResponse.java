package com.datavvy.vms.maintenance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MaintenanceResponse(
        Long id,
        Long vehicleId,
        String vehicleNumber,
        LocalDate serviceDate,
        MaintenanceType type,
        BigDecimal cost,
        String vendor,
        Integer odometerKm,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        long version) {
}
