package com.datavvy.vms.maintenance.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fleet maintenance figures for the dashboard. {@code byMonth} covers the last
 * six calendar months, oldest first, months without records included.
 */
public record MaintenanceStats(
        long totalRecords,
        BigDecimal totalCost,
        long recordsThisMonth,
        BigDecimal costThisMonth,
        long vehiclesServicedLast30Days,
        List<MonthCost> byMonth,
        List<TypeCost> byType) {

    /** {@code month} is {@code YYYY-MM}. */
    public record MonthCost(String month, long count, BigDecimal cost) {
    }

    public record TypeCost(MaintenanceType type, long count, BigDecimal cost) {
    }
}
