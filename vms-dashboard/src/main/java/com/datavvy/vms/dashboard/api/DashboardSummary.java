package com.datavvy.vms.dashboard.api;

import com.datavvy.vms.document.api.DocumentAlert;
import com.datavvy.vms.document.api.DocumentStats;
import com.datavvy.vms.driver.api.DriverStats;
import com.datavvy.vms.maintenance.api.MaintenanceResponse;
import com.datavvy.vms.maintenance.api.MaintenanceStats;
import com.datavvy.vms.vehicle.api.VehicleSummary;
import java.util.List;

/**
 * Everything the dashboard shows, composed from the vehicle, document,
 * maintenance and driver modules' public APIs. Fields after
 * {@code inMaintenance} were added for fleet management (additive, v1-compatible).
 */
public record DashboardSummary(
        Kpis kpis,
        List<Slice> byType,
        List<Slice> byBrand,
        List<VehicleSummary> recentlyUpdated,
        List<VehicleSummary> inMaintenance,
        DocumentStats documents,
        List<DocumentAlert> documentAlerts,
        MaintenanceStats maintenance,
        List<MaintenanceResponse> recentMaintenance,
        DriverStats drivers) {

    public record Kpis(long total, long active, long inactive, long maintenance) {
    }

    /** One bar: a value (type / brand) and its vehicle count. */
    public record Slice(String name, long count) {
    }
}
