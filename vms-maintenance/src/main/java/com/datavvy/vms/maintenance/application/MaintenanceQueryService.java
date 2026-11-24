package com.datavvy.vms.maintenance.application;

import com.datavvy.vms.maintenance.api.MaintenanceQueryApi;
import com.datavvy.vms.maintenance.api.MaintenanceResponse;
import com.datavvy.vms.maintenance.api.MaintenanceStats;
import com.datavvy.vms.maintenance.api.MaintenanceStats.MonthCost;
import com.datavvy.vms.maintenance.api.MaintenanceStats.TypeCost;
import com.datavvy.vms.maintenance.domain.MaintenanceRecord;
import com.datavvy.vms.maintenance.domain.MaintenanceRecordRepository;
import com.datavvy.vms.platform.config.PlatformProperties;
import com.datavvy.vms.vehicle.api.VehicleQueryApi;
import com.datavvy.vms.vehicle.api.VehicleSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class MaintenanceQueryService implements MaintenanceQueryApi {

    static final int MONTHS = 6;

    private final MaintenanceRecordRepository records;
    private final VehicleQueryApi vehicles;
    private final ZoneId zone;

    MaintenanceQueryService(MaintenanceRecordRepository records, VehicleQueryApi vehicles, PlatformProperties platform) {
        this.records = records;
        this.vehicles = vehicles;
        this.zone = platform.timeZone();
    }

    @Override
    public MaintenanceStats stats() {
        LocalDate today = LocalDate.now(zone);
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth firstMonth = thisMonth.minusMonths(MONTHS - 1);

        Map<YearMonth, long[]> counts = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> costs = new LinkedHashMap<>();
        for (int i = 0; i < MONTHS; i++) {
            counts.put(firstMonth.plusMonths(i), new long[1]);
            costs.put(firstMonth.plusMonths(i), BigDecimal.ZERO);
        }
        LocalDate since30 = today.minusDays(30);
        java.util.Set<Long> servicedRecently = new java.util.HashSet<>();
        for (MaintenanceRecord m : records.findByServiceDateGreaterThanEqual(firstMonth.atDay(1))) {
            YearMonth ym = YearMonth.from(m.getServiceDate());
            if (counts.containsKey(ym)) {
                counts.get(ym)[0]++;
                costs.merge(ym, m.getCost(), BigDecimal::add);
            }
            if (!m.getServiceDate().isBefore(since30)) {
                servicedRecently.add(m.getVehicleId());
            }
        }
        List<MonthCost> byMonth = new ArrayList<>();
        counts.forEach((ym, n) -> byMonth.add(new MonthCost(ym.toString(), n[0], money(costs.get(ym)))));

        List<TypeCost> byType = records.totalsByType().stream()
                .map(t -> new TypeCost(t.type(), t.count(), money(t.cost()))).toList();
        return new MaintenanceStats(records.count(), money(records.totalCost()),
                counts.get(thisMonth)[0], money(costs.get(thisMonth)), servicedRecently.size(), byMonth, byType);
    }

    @Override
    public List<MaintenanceResponse> recent(int limit) {
        List<MaintenanceRecord> latest = records.findLatest(PageRequest.of(0, limit));
        Map<Long, VehicleSummary> byId = vehicles.findAll(latest.stream().map(MaintenanceRecord::getVehicleId).toList());
        return latest.stream().map(m -> MaintenanceMapper.toResponse(m,
                byId.containsKey(m.getVehicleId()) ? byId.get(m.getVehicleId()).vehicleNumber() : null)).toList();
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}
