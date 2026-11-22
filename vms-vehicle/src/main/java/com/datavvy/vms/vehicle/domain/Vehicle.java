package com.datavvy.vms.vehicle.domain;

import com.datavvy.vms.platform.persistence.AuditableEntity;
import com.datavvy.vms.vehicle.api.VehicleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Fleet vehicle with the course fields: Vehicle Number, Company, Type, Brand,
 * Model, Status. {@code id} is a generated surrogate key; the business key is
 * the vehicle number, unique through {@code vehicle_number_key}.
 */
@Entity
@Table(name = "vehicle",
        uniqueConstraints = @UniqueConstraint(name = "uk_vehicle_number_key", columnNames = "vehicle_number_key"))
public class Vehicle extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display form, e.g. {@code DHA-METRO-GA 11-4582}. */
    @Column(name = "vehicle_number", nullable = false, length = 32)
    private String vehicleNumber;

    /** Normalised form, e.g. {@code DHAMETROGA114582}; UNIQUE in the database. */
    @Column(name = "vehicle_number_key", nullable = false, length = 32)
    private String vehicleNumberKey;

    @Column(nullable = false, length = 100)
    private String company;

    /** Name of a catalog vehicle type (validated through the catalog API). */
    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false, length = 60)
    private String brand;

    @Column(nullable = false, length = 60)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status;

    protected Vehicle() {
    }

    public Vehicle(VehicleDetails details) {
        apply(details);
    }

    /** Overwrites every editable field. */
    public void apply(VehicleDetails d) {
        this.vehicleNumber = d.vehicleNumber();
        this.vehicleNumberKey = VehicleNumber.key(d.vehicleNumber());
        this.company = d.company();
        this.type = d.type();
        this.brand = d.brand();
        this.model = d.model();
        this.status = d.status();
    }

    public Long getId() {
        return id;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public String getVehicleNumberKey() {
        return vehicleNumberKey;
    }

    public String getCompany() {
        return company;
    }

    public String getType() {
        return type;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public VehicleStatus getStatus() {
        return status;
    }
}
