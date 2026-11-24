package com.datavvy.vms.dashboard.application;

import com.datavvy.vms.catalog.api.CatalogKind;
import com.datavvy.vms.catalog.api.CatalogLookup;
import com.datavvy.vms.dashboard.api.DashboardSummary;
import com.datavvy.vms.dashboard.api.DashboardSummary.Kpis;
import com.datavvy.vms.dashboard.api.DashboardSummary.Slice;
import com.datavvy.vms.document.api.DocumentQueryApi;
import com.datavvy.vms.driver.api.DriverQueryApi;
import com.datavvy.vms.maintenance.api.MaintenanceQueryApi;
import com.datavvy.vms.vehicle.api.VehicleQueryApi;
import com.datavvy.vms.vehicle.api.VehicleStats;
import com.datavvy.vms.vehicle.api.VehicleStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    static final int LIST_LIMIT = 5;
    static final int TOP_BRANDS = 6;

    private final VehicleQueryApi vehicles;
    private final CatalogLookup catalog;
    private final DocumentQueryApi documents;
    private final MaintenanceQueryApi maintenance;
    private final DriverQueryApi drivers;

    public DashboardService(VehicleQueryApi vehicles, CatalogLookup catalog, DocumentQueryApi documents,
            MaintenanceQueryApi maintenance, DriverQueryApi drivers) {
        this.vehicles = vehicles;
        this.catalog = catalog;
        this.documents = documents;
        this.maintenance = maintenance;
        this.drivers = drivers;
    }

    public DashboardSummary summary() {
        VehicleStats s = vehicles.stats();
        return new DashboardSummary(
                new Kpis(s.total(), s.active(), s.inactive(), s.maintenance()),
                byType(s.byType()),
                s.byBrand().entrySet().stream().limit(TOP_BRANDS).map(e -> new Slice(e.getKey(), e.getValue())).toList(),
                vehicles.recentlyUpdated(LIST_LIMIT),
                vehicles.withStatus(VehicleStatus.MAINTENANCE, LIST_LIMIT),
                documents.stats(),
                documents.needingAttention(LIST_LIMIT),
                maintenance.stats(),
                maintenance.recent(LIST_LIMIT),
                drivers.stats());
    }

    /** Every catalog type (zero counts included), largest first. */
    private List<Slice> byType(Map<String, Long> counts) {
        Map<String, Long> lower = counts.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(Locale.ROOT), Map.Entry::getValue, Long::sum));
        return catalog.list(CatalogKind.VEHICLE_TYPE).stream()
                .map(ref -> new Slice(ref.name(), lower.getOrDefault(ref.name().toLowerCase(Locale.ROOT), 0L)))
                .sorted(Comparator.comparingLong(Slice::count).reversed().thenComparing(Slice::name))
                .toList();
    }
}
