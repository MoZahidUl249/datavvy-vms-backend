package com.datavvy.vms.maintenance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceRecordRepository
        extends JpaRepository<MaintenanceRecord, Long>, JpaSpecificationExecutor<MaintenanceRecord> {

    long countByVehicleId(Long vehicleId);

    @Query("select sum(m.cost) from MaintenanceRecord m where m.vehicleId = :vehicleId")
    BigDecimal totalCostForVehicle(@Param("vehicleId") Long vehicleId);

    @Query("select sum(m.cost) from MaintenanceRecord m "
            + "where m.vehicleId = :vehicleId and m.serviceDate >= :from")
    BigDecimal costForVehicleSince(@Param("vehicleId") Long vehicleId, @Param("from") LocalDate from);

    /** Latest record first (ties: latest entered first). */
    Optional<MaintenanceRecord> findFirstByVehicleIdOrderByServiceDateDescIdDesc(Long vehicleId);

    /** Highest earlier odometer reading of a vehicle, for the "odometer went backwards" check. */
    @Query("select max(m.odometerKm) from MaintenanceRecord m "
            + "where m.vehicleId = :vehicleId and m.serviceDate < :before and m.odometerKm is not null")
    Integer maxOdometerBefore(@Param("vehicleId") Long vehicleId, @Param("before") LocalDate before);

    @Query("select sum(m.cost) from MaintenanceRecord m")
    BigDecimal totalCost();

    List<MaintenanceRecord> findByServiceDateGreaterThanEqual(LocalDate from);

    @Query("select new com.datavvy.vms.maintenance.domain.TypeTotals(m.type, count(m), sum(m.cost)) "
            + "from MaintenanceRecord m group by m.type order by sum(m.cost) desc")
    List<TypeTotals> totalsByType();

    @Query("select m from MaintenanceRecord m order by m.serviceDate desc, m.id desc")
    List<MaintenanceRecord> findLatest(Pageable pageable);

    @Modifying
    @Query("delete from MaintenanceRecord m where m.vehicleId = :vehicleId")
    void deleteByVehicle(@Param("vehicleId") Long vehicleId);
}
