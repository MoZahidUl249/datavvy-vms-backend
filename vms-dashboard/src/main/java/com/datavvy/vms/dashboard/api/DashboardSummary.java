package com.datavvy.vms.dashboard.api;

import com.datavvy.vms.vehicle.api.VehicleSummary;
import java.util.List;

public record DashboardSummary(
        Kpis kpis,
        List<Slice> byType,
        List<Slice> byBrand,
        List<VehicleSummary> recentlyUpdated,
        List<VehicleSummary> inMaintenance) {

    public record Kpis(long total, long active, long inactive, long maintenance) {
    }

    /** One bar: a value (type / brand) and its vehicle count. */
    public record Slice(String name, long count) {
    }
}
