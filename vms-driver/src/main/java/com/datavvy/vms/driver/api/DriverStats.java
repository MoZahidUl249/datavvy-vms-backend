package com.datavvy.vms.driver.api;

/**
 * Driver figures for the dashboard. {@code available} = ACTIVE and not on a vehicle.
 * Licence counts cover every driver; {@code expiringSoonDays} is the window used.
 */
public record DriverStats(long total, long active, long onLeave, long inactive, long assigned, long available,
        long licenseExpiringSoon, long licenseExpired, int expiringSoonDays) {
}
