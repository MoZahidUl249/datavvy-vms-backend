package com.datavvy.vms.document.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleDocumentRepository
        extends JpaRepository<VehicleDocument, Long>, JpaSpecificationExecutor<VehicleDocument> {

    List<VehicleDocument> findByVehicleId(Long vehicleId);

    @Query("select d.id from VehicleDocument d where d.vehicleId = :vehicleId")
    List<Long> idsForVehicle(@Param("vehicleId") Long vehicleId);

    long countByExpiryDateIsNull();

    long countByExpiryDateBefore(LocalDate date);

    long countByExpiryDateBetween(LocalDate from, LocalDate to);

    long countByExpiryDateAfter(LocalDate date);

    /** Expired and expiring documents: expiry on or before {@code limit}, soonest first. */
    @Query("select d from VehicleDocument d where d.expiryDate <= :limit order by d.expiryDate asc, d.id asc")
    List<VehicleDocument> findExpiringOnOrBefore(@Param("limit") LocalDate limit, Pageable pageable);
}
