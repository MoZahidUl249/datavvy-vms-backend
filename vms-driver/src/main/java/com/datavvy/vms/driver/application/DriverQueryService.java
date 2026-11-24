package com.datavvy.vms.driver.application;

import com.datavvy.vms.driver.api.DriverQueryApi;
import com.datavvy.vms.driver.api.DriverStats;
import com.datavvy.vms.driver.api.DriverStatus;
import com.datavvy.vms.driver.config.DriverProperties;
import com.datavvy.vms.driver.domain.DriverAssignmentRepository;
import com.datavvy.vms.driver.domain.DriverRepository;
import com.datavvy.vms.platform.config.PlatformProperties;
import com.datavvy.vms.platform.time.Expiry;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DriverQueryService implements DriverQueryApi {

    private final DriverRepository drivers;
    private final DriverAssignmentRepository assignments;
    private final ZoneId zone;
    private final int soonDays;

    DriverQueryService(DriverRepository drivers, DriverAssignmentRepository assignments, PlatformProperties platform,
            DriverProperties props) {
        this.drivers = drivers;
        this.assignments = assignments;
        this.zone = platform.timeZone();
        this.soonDays = props.licenseExpiringSoonDays();
    }

    @Override
    public DriverStats stats() {
        LocalDate today = LocalDate.now(zone);
        long active = drivers.countByStatus(DriverStatus.ACTIVE);
        long onLeave = drivers.countByStatus(DriverStatus.ON_LEAVE);
        long inactive = drivers.countByStatus(DriverStatus.INACTIVE);
        return new DriverStats(active + onLeave + inactive, active, onLeave, inactive,
                assignments.countByOpenDriverIdIsNotNull(),
                drivers.countUnassignedWithStatus(DriverStatus.ACTIVE),
                drivers.countByLicenseExpiryBetween(today, Expiry.soonLimit(today, soonDays)),
                drivers.countByLicenseExpiryBefore(today), soonDays);
    }
}
