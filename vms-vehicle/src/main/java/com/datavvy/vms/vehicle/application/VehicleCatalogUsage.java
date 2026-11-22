package com.datavvy.vms.vehicle.application;

import com.datavvy.vms.catalog.api.CatalogItemRef;
import com.datavvy.vms.catalog.api.CatalogKind;
import com.datavvy.vms.catalog.api.CatalogLookup;
import com.datavvy.vms.catalog.api.CatalogUsageProvider;
import com.datavvy.vms.vehicle.domain.VehicleRepository;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Tells the catalog how many vehicles use each vehicle type (vehicles store the type name). */
@Component
@Transactional(readOnly = true)
class VehicleCatalogUsage implements CatalogUsageProvider {

    private final VehicleRepository vehicles;
    private final CatalogLookup catalog;

    VehicleCatalogUsage(VehicleRepository vehicles, CatalogLookup catalog) {
        this.vehicles = vehicles;
        this.catalog = catalog;
    }

    @Override
    public Map<Long, Long> usageCounts(CatalogKind kind) {
        if (kind != CatalogKind.VEHICLE_TYPE) {
            return Map.of();
        }
        Map<String, Long> byName = new HashMap<>();
        VehicleQueryService.toMap(vehicles.countByType())
                .forEach((name, n) -> byName.merge(name.toLowerCase(Locale.ROOT), n, Long::sum));
        Map<Long, Long> byId = new HashMap<>();
        for (CatalogItemRef ref : catalog.list(kind)) {
            byId.put(ref.id(), byName.getOrDefault(ref.name().toLowerCase(Locale.ROOT), 0L));
        }
        return byId;
    }
}
