package com.datavvy.vms.driver.domain;

import com.datavvy.vms.platform.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Driver ↔ vehicle assignment, kept as history. While open, {@code openVehicleId}
 * and {@code openDriverId} repeat the ids; both are UNIQUE, so the database
 * itself refuses two open assignments for one vehicle or one driver (NULLs don't
 * clash, so released rows never block). Assigned at/by = created at/by; released
 * by = the last updater once {@code releasedAt} is set.
 */
@Entity
@Table(name = "driver_assignment")
public class DriverAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false, updatable = false)
    private Long driverId;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private Long vehicleId;

    /** Snapshot for history. */
    @Column(name = "driver_name", nullable = false, length = 100, updatable = false)
    private String driverName;

    /** Snapshot for history. */
    @Column(name = "vehicle_number", nullable = false, length = 32, updatable = false)
    private String vehicleNumber;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "open_vehicle_id")
    private Long openVehicleId;

    @Column(name = "open_driver_id")
    private Long openDriverId;

    protected DriverAssignment() {
    }

    public static DriverAssignment open(Long driverId, String driverName, Long vehicleId, String vehicleNumber) {
        DriverAssignment a = new DriverAssignment();
        a.driverId = driverId;
        a.driverName = driverName;
        a.vehicleId = vehicleId;
        a.vehicleNumber = vehicleNumber;
        a.openVehicleId = vehicleId;
        a.openDriverId = driverId;
        return a;
    }

    public void release(Instant at) {
        this.releasedAt = at;
        this.openVehicleId = null;
        this.openDriverId = null;
    }

    public boolean isOpen() {
        return releasedAt == null;
    }

    public Long getId() {
        return id;
    }

    public Long getDriverId() {
        return driverId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }
}
