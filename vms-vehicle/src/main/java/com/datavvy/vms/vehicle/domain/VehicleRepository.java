package com.datavvy.vms.vehicle.domain;

import com.datavvy.vms.vehicle.api.VehicleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    /** Duplicate rule lookup (normalised number). */
    Optional<Vehicle> findByVehicleNumberKey(String vehicleNumberKey);

    boolean existsByVehicleNumberKey(String vehicleNumberKey);

    long countByStatus(VehicleStatus status);

    @Query("select distinct v.company from Vehicle v order by v.company")
    List<String> distinctCompanies();

    @Query("select distinct v.brand from Vehicle v order by v.brand")
    List<String> distinctBrands();

    @Query("select new com.datavvy.vms.vehicle.domain.TimestampPair(v.createdAt, v.updatedAt) from Vehicle v")
    List<TimestampPair> allTimestamps();

    @Query("select new com.datavvy.vms.vehicle.domain.NameCount(v.type, count(v)) from Vehicle v group by v.type")
    List<NameCount> countByType();

    @Query("select new com.datavvy.vms.vehicle.domain.NameCount(v.brand, count(v)) from Vehicle v "
            + "group by v.brand order by count(v) desc")
    List<NameCount> countByBrand();

    @Query("select v from Vehicle v order by v.updatedAt desc")
    List<Vehicle> findRecentlyUpdated(Pageable pageable);

    List<Vehicle> findByStatusOrderByUpdatedAtDesc(VehicleStatus status, Pageable pageable);
}
