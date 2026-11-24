package com.datavvy.vms.driver.domain;

import com.datavvy.vms.driver.api.DriverStatus;
import com.datavvy.vms.platform.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "driver")
public class Driver extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "license_number", nullable = false, length = 30)
    private String licenseNumber;

    /** Normalised licence number; UNIQUE in the database. */
    @Column(name = "license_number_key", nullable = false, length = 30)
    private String licenseNumberKey;

    @Column(name = "license_expiry", nullable = false)
    private LocalDate licenseExpiry;

    @Column(length = 200)
    private String address;

    @Column(name = "joined_on")
    private LocalDate joinedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriverStatus status;

    @Column(length = 500)
    private String notes;

    protected Driver() {
    }

    public Driver(DriverDetails details) {
        apply(details);
    }

    public void apply(DriverDetails d) {
        this.fullName = d.fullName();
        this.phone = d.phone();
        this.licenseNumber = d.licenseNumber();
        this.licenseNumberKey = LicenseNumber.key(d.licenseNumber());
        this.licenseExpiry = d.licenseExpiry();
        this.address = d.address();
        this.joinedOn = d.joinedOn();
        this.status = d.status();
        this.notes = d.notes();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getLicenseNumberKey() {
        return licenseNumberKey;
    }

    public LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public String getAddress() {
        return address;
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }
}
