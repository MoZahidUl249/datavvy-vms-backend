package com.datavvy.vms.driver.application;

import com.datavvy.vms.driver.api.AssignmentRecord;
import com.datavvy.vms.driver.api.DriverFilter;
import com.datavvy.vms.driver.api.DriverRequest;
import com.datavvy.vms.driver.api.DriverResponse;
import com.datavvy.vms.driver.config.DriverProperties;
import com.datavvy.vms.driver.domain.Driver;
import com.datavvy.vms.driver.domain.DriverAssignment;
import com.datavvy.vms.driver.domain.DriverAssignmentRepository;
import com.datavvy.vms.driver.domain.DriverDetails;
import com.datavvy.vms.driver.domain.DriverRepository;
import com.datavvy.vms.driver.domain.DriverSpecifications;
import com.datavvy.vms.driver.domain.LicenseNumber;
import com.datavvy.vms.driver.domain.PhoneNumber;
import com.datavvy.vms.platform.config.PlatformProperties;
import com.datavvy.vms.platform.error.ConflictException;
import com.datavvy.vms.platform.error.FieldValidationException;
import com.datavvy.vms.platform.error.FieldValidationException.FieldMessage;
import com.datavvy.vms.platform.error.NotFoundException;
import com.datavvy.vms.platform.web.PageResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Driver use cases. Rules:
 * <ul>
 *   <li>Licence number is unique ignoring case, spaces, hyphens and slashes
 *       ({@code DUPLICATE_LICENSE_NUMBER}); service check plus UNIQUE constraint.</li>
 *   <li>Mobile number must be a Bangladeshi mobile (stored as 01XXXXXXXXX).</li>
 *   <li>A driver who is on a vehicle can't be deleted ({@code DRIVER_ASSIGNED}); unassign first.</li>
 *   <li>Updates send the version they loaded.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DriverService {

    public static final String DUPLICATE_LICENSE_NUMBER = "DUPLICATE_LICENSE_NUMBER";

    private static final Set<String> SORTABLE = Set.of(
            "fullName", "licenseExpiry", "status", "joinedOn", "createdAt", "updatedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "fullName");

    private final DriverRepository drivers;
    private final DriverAssignmentRepository assignments;
    private final ZoneId zone;
    private final int soonDays;

    public DriverService(DriverRepository drivers, DriverAssignmentRepository assignments,
            PlatformProperties platform, DriverProperties props) {
        this.drivers = drivers;
        this.assignments = assignments;
        this.zone = platform.timeZone();
        this.soonDays = props.licenseExpiringSoonDays();
    }

    // ---------------------------------------------------------------- queries

    public PageResponse<DriverResponse> search(DriverFilter filter, Pageable pageable) {
        Page<Driver> page = drivers.findAll(DriverSpecifications.matching(filter), safe(pageable));
        Map<Long, DriverAssignment> open = assignments
                .findByOpenDriverIdIn(page.getContent().stream().map(Driver::getId).toList()).stream()
                .collect(Collectors.toMap(DriverAssignment::getDriverId, Function.identity()));
        LocalDate today = today();
        return PageResponse.of(page, d -> DriverMapper.toResponse(d, open.get(d.getId()), today, soonDays));
    }

    public DriverResponse get(Long id) {
        return toResponse(load(id));
    }

    public List<AssignmentRecord> assignmentHistory(Long driverId) {
        load(driverId);
        return assignments.findByDriverIdOrderByCreatedAtDescIdDesc(driverId).stream()
                .map(DriverMapper::toRecord).toList();
    }

    // --------------------------------------------------------------- commands

    @Transactional
    public DriverResponse create(DriverRequest request) {
        DriverDetails details = validate(request);
        rejectDuplicateLicense(details.licenseNumber(), null);
        return toResponse(saveGuarded(new Driver(details)));
    }

    @Transactional
    public DriverResponse update(Long id, DriverRequest request) {
        Driver driver = load(id);
        if (request.version() == null || request.version() != driver.getVersion()) {
            throw new ConflictException("STALE_VERSION", "Someone else changed this driver. Reload and try again.",
                    Map.of("currentVersion", driver.getVersion()));
        }
        DriverDetails details = validate(request);
        rejectDuplicateLicense(details.licenseNumber(), id);
        driver.apply(details);
        return toResponse(saveGuarded(driver));
    }

    @Transactional
    public void delete(Long id) {
        Driver driver = load(id);
        assignments.findByOpenDriverId(id).ifPresent(a -> {
            throw new ConflictException("DRIVER_ASSIGNED",
                    driver.getFullName() + " is driving " + a.getVehicleNumber() + ". Unassign them from the vehicle first.",
                    Map.of("vehicleId", a.getVehicleId(), "vehicleNumber", a.getVehicleNumber()));
        });
        drivers.delete(driver);
    }

    // ---------------------------------------------------------------- helpers

    DriverDetails validate(DriverRequest r) {
        List<FieldMessage> errors = new ArrayList<>();
        String phone = PhoneNumber.canonical(r.phone()).orElse(null);
        if (phone == null) {
            errors.add(new FieldMessage("phone", "Enter a Bangladeshi mobile number, like 01712345678."));
        }
        if (!LicenseNumber.looksValid(r.licenseNumber())) {
            errors.add(new FieldMessage("licenseNumber",
                    "Enter the licence number as printed (5–30 letters and digits), like DK0123456C00001."));
        }
        if (r.joinedOn() != null && r.joinedOn().isAfter(today())) {
            errors.add(new FieldMessage("joinedOn", "The joining date can't be in the future."));
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        return new DriverDetails(tidy(r.fullName()), phone, LicenseNumber.display(r.licenseNumber()),
                r.licenseExpiry(), tidy(r.address()), r.joinedOn(), r.status(),
                r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
    }

    private void rejectDuplicateLicense(String licenseNumber, Long selfId) {
        drivers.findByLicenseNumberKey(LicenseNumber.key(licenseNumber))
                .filter(other -> !Objects.equals(other.getId(), selfId))
                .ifPresent(other -> {
                    throw duplicate(licenseNumber, other);
                });
    }

    private static ConflictException duplicate(String licenseNumber, Driver other) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("field", "licenseNumber");
        if (other != null) {
            details.put("existingId", other.getId());
            details.put("existingName", other.getFullName());
        }
        return new ConflictException(DUPLICATE_LICENSE_NUMBER,
                "Licence " + licenseNumber + " already belongs to "
                        + (other == null ? "another driver" : other.getFullName()) + ".", details);
    }

    private Driver saveGuarded(Driver driver) {
        try {
            return drivers.saveAndFlush(driver);
        } catch (DataIntegrityViolationException e) {
            throw duplicate(driver.getLicenseNumber(), null);
        }
    }

    private DriverResponse toResponse(Driver d) {
        DriverAssignment open = d.getId() == null ? null : assignments.findByOpenDriverId(d.getId()).orElse(null);
        return DriverMapper.toResponse(d, open, today(), soonDays);
    }

    private Driver load(Long id) {
        return drivers.findById(id).orElseThrow(() -> new NotFoundException("Driver", id));
    }

    private LocalDate today() {
        return LocalDate.now(zone);
    }

    private static String tidy(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    private static Pageable safe(Pageable p) {
        List<Sort.Order> orders = p.getSort().stream().filter(o -> SORTABLE.contains(o.getProperty())).toList();
        Sort sort = (orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders)).and(Sort.by("id"));
        return PageRequest.of(p.getPageNumber(), p.getPageSize(), sort);
    }
}
