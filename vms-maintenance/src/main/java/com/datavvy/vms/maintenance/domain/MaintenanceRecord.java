package com.datavvy.vms.maintenance.domain;

import com.datavvy.vms.maintenance.api.MaintenanceType;
import com.datavvy.vms.platform.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One service or repair of one vehicle ({@code vehicleId} by id only, ADR-0003). */
@Entity
@Table(name = "maintenance_record")
public class MaintenanceRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private Long vehicleId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MaintenanceType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false, length = 120)
    private String vendor;

    @Column(name = "odometer_km")
    private Integer odometerKm;

    @Column(length = 1000)
    private String notes;

    protected MaintenanceRecord() {
    }

    public MaintenanceRecord(Long vehicleId, MaintenanceDetails details) {
        this.vehicleId = vehicleId;
        apply(details);
    }

    public void apply(MaintenanceDetails d) {
        this.serviceDate = d.serviceDate();
        this.type = d.type();
        this.cost = d.cost();
        this.vendor = d.vendor();
        this.odometerKm = d.odometerKm();
        this.notes = d.notes();
    }

    public Long getId() {
        return id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public MaintenanceType getType() {
        return type;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public String getVendor() {
        return vendor;
    }

    public Integer getOdometerKm() {
        return odometerKm;
    }

    public String getNotes() {
        return notes;
    }
}
