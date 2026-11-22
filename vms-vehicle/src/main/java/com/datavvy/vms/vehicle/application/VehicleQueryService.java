package com.datavvy.vms.vehicle.application;

import com.datavvy.vms.vehicle.api.VehicleQueryApi;
import com.datavvy.vms.vehicle.api.VehicleStats;
import com.datavvy.vms.vehicle.api.VehicleStatus;
import com.datavvy.vms.vehicle.api.VehicleSummary;
import com.datavvy.vms.vehicle.domain.NameCount;
import com.datavvy.vms.vehicle.domain.VehicleRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class VehicleQueryService implements VehicleQueryApi {

    private final VehicleRepository vehicles;

    VehicleQueryService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    @Override
    public VehicleStats stats() {
        long active = vehicles.countByStatus(VehicleStatus.ACTIVE);
        long inactive = vehicles.countByStatus(VehicleStatus.INACTIVE);
        long maintenance = vehicles.countByStatus(VehicleStatus.MAINTENANCE);
        return new VehicleStats(active + inactive + maintenance, active, inactive, maintenance,
                toMap(vehicles.countByType()), toMap(vehicles.countByBrand()));
    }

    @Override
    public List<VehicleSummary> recentlyUpdated(int limit) {
        return vehicles.findRecentlyUpdated(PageRequest.of(0, limit)).stream().map(VehicleMapper::toSummary).toList();
    }

    @Override
    public List<VehicleSummary> withStatus(VehicleStatus status, int limit) {
        return vehicles.findByStatusOrderByUpdatedAtDesc(status, PageRequest.of(0, limit)).stream()
                .map(VehicleMapper::toSummary).toList();
    }

    static Map<String, Long> toMap(List<NameCount> counts) {
        Map<String, Long> map = new LinkedHashMap<>();
        counts.forEach(c -> map.put(c.name(), c.count()));
        return map;
    }
}
