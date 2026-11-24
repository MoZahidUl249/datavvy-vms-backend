package com.datavvy.vms.driver.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverAssignmentRepository extends JpaRepository<DriverAssignment, Long> {

    Optional<DriverAssignment> findByOpenVehicleId(Long vehicleId);

    Optional<DriverAssignment> findByOpenDriverId(Long driverId);

    List<DriverAssignment> findByOpenDriverIdIn(Collection<Long> driverIds);

    long countByOpenDriverIdIsNotNull();

    List<DriverAssignment> findByVehicleIdOrderByCreatedAtDescIdDesc(Long vehicleId);

    List<DriverAssignment> findByDriverIdOrderByCreatedAtDescIdDesc(Long driverId);
}
