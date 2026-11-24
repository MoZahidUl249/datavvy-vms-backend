package com.datavvy.vms.maintenance.application;

import com.datavvy.vms.maintenance.api.MaintenanceFilter;
import com.datavvy.vms.maintenance.api.MaintenanceHistorySummary;
import com.datavvy.vms.maintenance.api.MaintenanceRequest;
import com.datavvy.vms.maintenance.api.MaintenanceResponse;
import com.datavvy.vms.maintenance.domain.MaintenanceDetails;
import com.datavvy.vms.maintenance.domain.MaintenanceRecord;
import com.datavvy.vms.maintenance.domain.MaintenanceRecordRepository;
import com.datavvy.vms.maintenance.domain.MaintenanceSpecifications;
import com.datavvy.vms.platform.config.PlatformProperties;
import com.datavvy.vms.platform.error.ConflictException;
import com.datavvy.vms.platform.error.FieldValidationException;
import com.datavvy.vms.platform.error.FieldValidationException.FieldMessage;
import com.datavvy.vms.platform.error.NotFoundException;
import com.datavvy.vms.platform.web.PageResponse;
import com.datavvy.vms.vehicle.api.VehicleQueryApi;
import com.datavvy.vms.vehicle.api.VehicleSummary;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintenance use cases. Rules:
 * <ul>
 *   <li>The vehicle must exist (through {@code vehicle.api}).</li>
 *   <li>The service date can't be in the future (business time zone).</li>
 *   <li>An odometer reading can't be lower than one recorded on an earlier date.</li>
 *   <li>Updates send the version they loaded (optimistic locking).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class MaintenanceService {

    private static final Set<String> SORTABLE = Set.of("serviceDate", "cost", "type", "vendor", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "serviceDate");

    private final MaintenanceRecordRepository records;
    private final VehicleQueryApi vehicles;
    private final ZoneId zone;

    public MaintenanceService(MaintenanceRecordRepository records, VehicleQueryApi vehicles,
            PlatformProperties platform) {
        this.records = records;
        this.vehicles = vehicles;
        this.zone = platform.timeZone();
    }

    // ---------------------------------------------------------------- queries

    /** A vehicle's maintenance history, newest first by default. */
    public PageResponse<MaintenanceResponse> history(Long vehicleId, Pageable pageable) {
        VehicleSummary vehicle = requireVehicle(vehicleId);
        Page<MaintenanceRecord> page = records.findAll(
                MaintenanceSpecifications.matching(new MaintenanceFilter(vehicleId, null, null, null, null), Set.of()),
                safe(pageable));
        return PageResponse.of(page, m -> MaintenanceMapper.toResponse(m, vehicle.vehicleNumber()));
    }

    public MaintenanceHistorySummary historySummary(Long vehicleId) {
        requireVehicle(vehicleId);
        Optional<MaintenanceRecord> last = records.findFirstByVehicleIdOrderByServiceDateDescIdDesc(vehicleId);
        return new MaintenanceHistorySummary(vehicleId,
                records.countByVehicleId(vehicleId),
                money(records.totalCostForVehicle(vehicleId)),
                money(records.costForVehicleSince(vehicleId, today().minusMonths(12))),
                last.map(MaintenanceRecord::getServiceDate).orElse(null),
                last.map(MaintenanceRecord::getType).orElse(null),
                last.map(MaintenanceRecord::getOdometerKm).orElse(null));
    }

    public PageResponse<MaintenanceResponse> search(MaintenanceFilter filter, Pageable pageable) {
        Set<Long> idsForQ = filter.q() == null || filter.q().isBlank() ? Set.of() : vehicles.idsWithNumberLike(filter.q());
        Page<MaintenanceRecord> page = records.findAll(MaintenanceSpecifications.matching(filter, idsForQ), safe(pageable));
        Map<Long, VehicleSummary> byId = vehicles.findAll(page.getContent().stream().map(MaintenanceRecord::getVehicleId).toList());
        return PageResponse.of(page, m -> MaintenanceMapper.toResponse(m,
                byId.containsKey(m.getVehicleId()) ? byId.get(m.getVehicleId()).vehicleNumber() : null));
    }

    public MaintenanceResponse get(Long id) {
        return toResponse(load(id));
    }

    // --------------------------------------------------------------- commands

    @Transactional
    public MaintenanceResponse create(Long vehicleId, MaintenanceRequest request) {
        VehicleSummary vehicle = requireVehicle(vehicleId);
        MaintenanceDetails details = validate(vehicleId, request);
        return MaintenanceMapper.toResponse(records.saveAndFlush(new MaintenanceRecord(vehicleId, details)),
                vehicle.vehicleNumber());
    }

    @Transactional
    public MaintenanceResponse update(Long id, MaintenanceRequest request) {
        MaintenanceRecord record = load(id);
        if (request.version() == null || request.version() != record.getVersion()) {
            throw new ConflictException("STALE_VERSION",
                    "Someone else changed this record. Reload it and try again.",
                    Map.of("currentVersion", record.getVersion()));
        }
        record.apply(validate(record.getVehicleId(), request));
        return toResponse(records.saveAndFlush(record));
    }

    @Transactional
    public void delete(Long id) {
        records.delete(load(id));
    }

    // ---------------------------------------------------------------- helpers

    MaintenanceDetails validate(Long vehicleId, MaintenanceRequest r) {
        List<FieldMessage> errors = new ArrayList<>();
        if (r.serviceDate() != null && r.serviceDate().isAfter(today())) {
            errors.add(new FieldMessage("serviceDate", "The service date can't be in the future; record work once it's done."));
        }
        if (r.odometerKm() != null && r.serviceDate() != null) {
            Integer earlier = records.maxOdometerBefore(vehicleId, r.serviceDate());
            if (earlier != null && r.odometerKm() < earlier) {
                errors.add(new FieldMessage("odometerKm",
                        "An earlier service recorded " + earlier + " km; the odometer can't go backwards."));
            }
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        return new MaintenanceDetails(r.serviceDate(), r.type(), r.cost().setScale(2, RoundingMode.HALF_UP),
                tidy(r.vendor()), r.odometerKm(), r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
    }

    private MaintenanceResponse toResponse(MaintenanceRecord m) {
        return MaintenanceMapper.toResponse(m,
                vehicles.find(m.getVehicleId()).map(VehicleSummary::vehicleNumber).orElse(null));
    }

    private VehicleSummary requireVehicle(Long vehicleId) {
        return vehicles.find(vehicleId).orElseThrow(() -> new NotFoundException("Vehicle", vehicleId));
    }

    private MaintenanceRecord load(Long id) {
        return records.findById(id).orElseThrow(() -> new NotFoundException("Maintenance", id));
    }

    private LocalDate today() {
        return LocalDate.now(zone);
    }

    private static java.math.BigDecimal money(java.math.BigDecimal v) {
        return (v == null ? java.math.BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static String tidy(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }

    private static Pageable safe(Pageable p) {
        List<Sort.Order> orders = p.getSort().stream().filter(o -> SORTABLE.contains(o.getProperty())).toList();
        Sort sort = (orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders)).and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(p.getPageNumber(), p.getPageSize(), sort);
    }
}
