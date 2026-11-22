package com.datavvy.vms.platform.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code vms.*} settings shared across modules.
 *
 * @param timeZone business time zone used to turn instants into calendar days
 *                 (filters such as "Added on 12 Nov"). Defaults to Asia/Dhaka.
 */
@ConfigurationProperties(prefix = "vms")
public record PlatformProperties(ZoneId timeZone) {

    public PlatformProperties {
        if (timeZone == null) {
            timeZone = ZoneId.of("Asia/Dhaka");
        }
    }
}
