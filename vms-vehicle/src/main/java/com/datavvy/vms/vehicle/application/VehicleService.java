package com.datavvy.vms.vehicle.application;

import com.datavvy.vms.catalog.api.CatalogItemRef;
import com.datavvy.vms.catalog.api.CatalogKind;
import com.datavvy.vms.catalog.api.CatalogLookup;
import com.datavvy.vms.platform.config.PlatformProperties;
import com.datavvy.vms.platform.error.ConflictException;
import com.datavvy.vms.platform.error.FieldValidationException;
import com.datavvy.vms.platform.error.FieldValidationException.FieldMessage;
import com.datavvy.vms.platform.error.NotFoundException;
import com.datavvy.vms.platform.web.PageResponse;
import com.datavvy.vms.vehicle.api.VehicleDeletedEvent;
import com.datavvy.vms.vehicle.api.VehicleFacets;
import com.datavvy.vms.vehicle.api.VehicleFilter;
import com.datavvy.vms.vehicle.api.VehicleNumberCheckResponse;
import com.datavvy.vms.vehicle.api.VehicleRequest;
import com.datavvy.vms.vehicle.api.VehicleResponse;
import com.datavvy.vms.vehicle.api.VehicleStatus;
import com.datavvy.vms.vehicle.domain.TimestampPair;
import com.datavvy.vms.vehicle.domain.Vehicle;
import com.datavvy.vms.vehicle.domain.VehicleDetails;
import com.datavvy.vms.vehicle.domain.VehicleNumber;
import com.datavvy.vms.vehicle.domain.VehicleRepository;
import com.datavvy.vms.vehicle.domain.VehicleSpecifications;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vehicle use cases. Business rules live here, not in the controller:
 * <ul>
 *   <li><b>Duplicate rule</b> — a Vehicle Number may exist only once (compared
 *       ignoring case, spaces and hyphens). Checked before saving, and again by
 *       the database UNIQUE constraint for concurrent requests.</li>
 *   <li>Vehicle Number must be a valid BD number; Type must be a catalog type.</li>
 *   <li>Updates must send the version they loaded (optimistic locking).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class VehicleService {

    private static final Set<String> SORTABLE = Set.of(
            "vehicleNumber", "company", "type", "brand", "model", "status", "createdAt", "updatedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "updatedAt");

    private final VehicleRepository vehicles;
    private final CatalogLookup catalog;
    private final ZoneId zone;
    private final ApplicationEventPublisher events;

    public VehicleService(VehicleRepository vehicles, CatalogLookup catalog, PlatformProperties platform,
            ApplicationEventPublisher events) {
        this.vehicles = vehicles;
        this.catalog = catalog;
        this.zone = platform.timeZone();
        this.events = events;
    }

    // ---------------------------------------------------------------- queries

    public PageResponse<VehicleResponse> search(VehicleFilter filter, Pageable pageable) {
        return PageResponse.of(vehicles.findAll(VehicleSpecifications.matching(filter, zone), safe(pageable)),
                VehicleMapper::toResponse);
    }

    public VehicleResponse get(Long id) {
        return VehicleMapper.toResponse(load(id));
    }

    public VehicleFacets facets() {
        long active = vehicles.countByStatus(VehicleStatus.ACTIVE);
        long inactive = vehicles.countByStatus(VehicleStatus.INACTIVE);
        long maintenance = vehicles.countByStatus(VehicleStatus.MAINTENANCE);
        Set<LocalDate> added = new TreeSet<>();
        Set<LocalDate> updated = new TreeSet<>();
        for (TimestampPair t : vehicles.allTimestamps()) {
            added.add(LocalDate.ofInstant(t.createdAt(), zone));
            updated.add(LocalDate.ofInstant(t.updatedAt(), zone));
        }
        return new VehicleFacets(
                new VehicleFacets.StatusCounts(active + inactive + maintenance, active, inactive, maintenance),
                vehicles.distinctCompanies(), vehicles.distinctBrands(),
                new ArrayList<>(updated), new ArrayList<>(added));
    }

    /** Live check for the form; the authoritative check runs again on save. */
    public VehicleNumberCheckResponse checkVehicleNumber(String raw, Long excludeId) {
        Optional<String> canonical = VehicleNumber.canonical(raw);
        if (canonical.isEmpty()) {
            return new VehicleNumberCheckResponse(false, false, null, null, null);
        }
        return findClash(canonical.get(), excludeId)
                .map(v -> new VehicleNumberCheckResponse(true, false, canonical.get(), v.getId(), v.getVehicleNumber()))
                .orElseGet(() -> new VehicleNumberCheckResponse(true, true, canonical.get(), null, null));
    }

    // --------------------------------------------------------------- commands

    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        VehicleDetails details = validate(request);
        rejectDuplicate(details.vehicleNumber(), null);
        return VehicleMapper.toResponse(saveGuarded(new Vehicle(details)));
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle vehicle = load(id);
        if (request.version() == null || request.version() != vehicle.getVersion()) {
            throw new ConflictException("STALE_VERSION",
                    "Someone else changed this vehicle. Reload it and try again.",
                    Map.of("currentVersion", vehicle.getVersion()));
        }
        VehicleDetails details = validate(request);
        rejectDuplicate(details.vehicleNumber(), id);
        vehicle.apply(details);
        return VehicleMapper.toResponse(saveGuarded(vehicle));
    }

    /**
     * Deletes the vehicle and tells other modules (documents, maintenance,
     * driver assignments) in the same transaction, so they clean up their
     * records or the whole delete rolls back.
     */
    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = load(id);
        vehicles.delete(vehicle);
        events.publishEvent(new VehicleDeletedEvent(vehicle.getId(), vehicle.getVehicleNumber()));
    }

    // ---------------------------------------------------------------- helpers

    /** The duplicate Vehicle Number rule. */
    void rejectDuplicate(String vehicleNumber, Long selfId) {
        findClash(vehicleNumber, selfId).ifPresent(other -> {
            throw new DuplicateVehicleNumberException(other.getVehicleNumber(), other.getId());
        });
    }

    private Optional<Vehicle> findClash(String vehicleNumber, Long excludeId) {
        return vehicles.findByVehicleNumberKey(VehicleNumber.key(vehicleNumber))
                .filter(v -> !Objects.equals(v.getId(), excludeId));
    }

    private VehicleDetails validate(VehicleRequest r) {
        List<FieldMessage> errors = new ArrayList<>();

        String number = VehicleNumber.canonical(r.vehicleNumber()).orElse(null);
        if (number == null) {
            errors.add(new FieldMessage("vehicleNumber", "Enter the full vehicle number, like DHA-METRO-GA 11-4582."));
        }
        Optional<CatalogItemRef> type = catalog.findByName(CatalogKind.VEHICLE_TYPE, r.type());
        if (type.isEmpty()) {
            errors.add(new FieldMessage("type", "Choose a vehicle type from the list, like Microbus."));
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        return new VehicleDetails(number, tidy(r.company()), type.get().name(), tidy(r.brand()), tidy(r.model()),
                r.status());
    }

    /** Backstop for two requests saving the same number at once: the UNIQUE constraint fires. */
    private Vehicle saveGuarded(Vehicle vehicle) {
        try {
            return vehicles.saveAndFlush(vehicle);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateVehicleNumberException(vehicle.getVehicleNumber(), null);
        }
    }

    private Vehicle load(Long id) {
        return vehicles.findById(id).orElseThrow(() -> new NotFoundException("Vehicle", id));
    }

    private static String tidy(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }

    /** Drops unknown sort properties instead of failing with a 500. */
    private static Pageable safe(Pageable p) {
        List<Sort.Order> orders = p.getSort().stream().filter(o -> SORTABLE.contains(o.getProperty())).toList();
        Sort sort = orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders).and(Sort.by("id"));
        return PageRequest.of(p.getPageNumber(), p.getPageSize(), sort);
    }
}
