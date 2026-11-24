package com.datavvy.vms.driver.domain;

import com.datavvy.vms.driver.api.DriverStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, Long>, JpaSpecificationExecutor<Driver> {

    Optional<Driver> findByLicenseNumberKey(String licenseNumberKey);

    long countByStatus(DriverStatus status);

    long countByLicenseExpiryBefore(LocalDate date);

    long countByLicenseExpiryBetween(LocalDate from, LocalDate to);

    /** Drivers with {@code status} and no open assignment. */
    @Query("select count(d) from Driver d where d.status = :status "
            + "and not exists (select a.id from DriverAssignment a where a.openDriverId = d.id)")
    long countUnassignedWithStatus(@Param("status") DriverStatus status);
}
