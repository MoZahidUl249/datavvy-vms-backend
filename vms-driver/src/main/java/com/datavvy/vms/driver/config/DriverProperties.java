package com.datavvy.vms.driver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code vms.drivers.*}
 *
 * @param licenseExpiringSoonDays a licence is "expiring soon" this many days before expiry (default 30)
 */
@ConfigurationProperties(prefix = "vms.drivers")
public record DriverProperties(Integer licenseExpiringSoonDays) {

    public DriverProperties {
        if (licenseExpiringSoonDays == null) {
            licenseExpiringSoonDays = 30;
        }
    }
}
