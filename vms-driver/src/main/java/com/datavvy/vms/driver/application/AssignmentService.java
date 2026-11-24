package com.datavvy.vms.driver.application;

import com.datavvy.vms.driver.api.AssignmentRecord;
import com.datavvy.vms.driver.api.AssignmentRequest;
import com.datavvy.vms.driver.api.DriverStatus;
import com.datavvy.vms.driver.api.VehicleAssignment;
import com.datavvy.vms.driver.config.DriverProperties;
import com.datavvy.vms.driver.domain.Driver;
import com.datavvy.vms.driver.domain.DriverAssignment;
import com.datavvy.vms.driver.domain.DriverAssignmentRepository;
import com.datavvy.vms.driver.domain.DriverRepository;
import com.datavvy.vms.platform.config.PlatformProperties;
import com.datavvy.vms.platform.error.BusinessRuleException;
import com.datavvy.vms.platform.error.ConflictException;
import com.datavvy.vms.platform.error.NotFoundException;
import com.datavvy.vms.vehicle.api.VehicleQueryApi;
import com.datavvy.vms.vehicle.api.VehicleStatus;
import com.datavvy.vms.vehicle.api.VehicleSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Driver ↔ vehicle assignment. Rules:
 * <ul>
 *   <li>One open assignment per vehicle and per driver.</li>
 *   <li>INACTIVE vehicles can't get a driver ({@code VEHICLE_INACTIVE}, 422).</li>
 *   <li>Only ACTIVE drivers ({@code DRIVER_NOT_ACTIVE}, 422) with an unexpired licence
 *       ({@code DRIVER_LICENSE_EXPIRED}, 422).</li>
 *   <li>A driver on another vehicle is moved only when the request says {@code reassign}
 *       ({@code DRIVER_ASSIGNED_ELSEWHERE}, 409).</li>
 *   <li>Changing the driver closes the previous assignment; history is kept.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final DriverAssignmentRepository assignments;
    private final DriverRepository drivers;
    private final VehicleQueryApi vehicles;
    private final ZoneId zone;
    private final int soonDays;

    public AssignmentService(DriverAssignmentRepository assignments, DriverRepository drivers,
            VehicleQueryApi vehicles, PlatformProperties platform, DriverProperties props) {
        this.assignments = assignments;
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.zone = platform.timeZone();
        this.soonDays = props.licenseExpiringSoonDays();
    }

    public VehicleAssignment current(Long vehicleId) {
        requireVehicle(vehicleId);
        return view(vehicleId, assignments.findByOpenVehicleId(vehicleId));
    }

    public List<AssignmentRecord> history(Long vehicleId) {
        requireVehicle(vehicleId);
        return assignments.findByVehicleIdOrderByCreatedAtDescIdDesc(vehicleId).stream()
                .map(DriverMapper::toRecord).toList();
    }

    @Transactional
    public VehicleAssignment assign(Long vehicleId, AssignmentRequest request) {
        VehicleSummary vehicle = requireVehicle(vehicleId);
        if (vehicle.status() == VehicleStatus.INACTIVE) {
            throw new BusinessRuleException("VEHICLE_INACTIVE",
                    vehicle.vehicleNumber() + " is inactive. Set it to Active or Maintenance before assigning a driver.");
        }
        Driver driver = drivers.findById(request.driverId())
                .orElseThrow(() -> new NotFoundException("Driver", request.driverId()));
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new BusinessRuleException("DRIVER_NOT_ACTIVE",
                    driver.getFullName() + " is " + label(driver.getStatus()) + ". Only active drivers can be assigned.",
                    Map.of("driverId", driver.getId(), "status", driver.getStatus().name()));
        }
        if (driver.getLicenseExpiry().isBefore(LocalDate.now(zone))) {
            throw new BusinessRuleException("DRIVER_LICENSE_EXPIRED",
                    driver.getFullName() + "'s licence expired on " + driver.getLicenseExpiry()
                            + ". Update the licence before assigning them.",
                    Map.of("driverId", driver.getId(), "licenseExpiry", driver.getLicenseExpiry().toString()));
        }

        Optional<DriverAssignment> onThisVehicle = assignments.findByOpenVehicleId(vehicleId);
        if (onThisVehicle.isPresent() && Objects.equals(onThisVehicle.get().getDriverId(), driver.getId())) {
            return view(vehicleId, onThisVehicle);
        }
        Optional<DriverAssignment> elsewhere = assignments.findByOpenDriverId(driver.getId());
        if (elsewhere.isPresent() && !request.reassign()) {
            DriverAssignment other = elsewhere.get();
            throw new ConflictException("DRIVER_ASSIGNED_ELSEWHERE",
                    driver.getFullName() + " is driving " + other.getVehicleNumber()
                            + ". Move them to " + vehicle.vehicleNumber() + "?",
                    Map.of("vehicleId", other.getVehicleId(), "vehicleNumber", other.getVehicleNumber()));
        }

        Instant now = Instant.now();
        onThisVehicle.ifPresent(a -> a.release(now));
        elsewhere.ifPresent(a -> a.release(now));
        assignments.flush();
        try {
            DriverAssignment created = assignments.saveAndFlush(
                    DriverAssignment.open(driver.getId(), driver.getFullName(), vehicleId, vehicle.vehicleNumber()));
            return view(vehicleId, Optional.of(created));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("ASSIGNMENT_CONFLICT",
                    "Someone else changed this assignment at the same time. Reload and try again.");
        }
    }

    @Transactional
    public void unassign(Long vehicleId) {
        requireVehicle(vehicleId);
        assignments.findByOpenVehicleId(vehicleId).ifPresent(a -> {
            a.release(Instant.now());
            assignments.saveAndFlush(a);
        });
    }

    /** Vehicle deleted: close its assignment, keep the history (names are snapshots). */
    @Transactional
    public void releaseForDeletedVehicle(Long vehicleId) {
        assignments.findByOpenVehicleId(vehicleId).ifPresent(a -> {
            a.release(Instant.now());
            assignments.saveAndFlush(a);
        });
    }

    private VehicleAssignment view(Long vehicleId, Optional<DriverAssignment> open) {
        if (open.isEmpty()) {
            return new VehicleAssignment(vehicleId, null, null, null);
        }
        DriverAssignment a = open.get();
        LocalDate today = LocalDate.now(zone);
        return drivers.findById(a.getDriverId())
                .map(d -> new VehicleAssignment(vehicleId, DriverMapper.toSummary(d, today, soonDays),
                        a.getCreatedAt(), a.getCreatedBy()))
                .orElseGet(() -> new VehicleAssignment(vehicleId, null, null, null));
    }

    private VehicleSummary requireVehicle(Long vehicleId) {
        return vehicles.find(vehicleId).orElseThrow(() -> new NotFoundException("Vehicle", vehicleId));
    }

    private static String label(DriverStatus s) {
        return s == DriverStatus.ON_LEAVE ? "on leave" : s.name().toLowerCase(java.util.Locale.ROOT);
    }
}
