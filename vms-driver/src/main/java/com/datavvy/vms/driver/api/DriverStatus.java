package com.datavvy.vms.driver.api;

/** Only ACTIVE drivers can be assigned to a vehicle. */
public enum DriverStatus {
    ACTIVE,
    ON_LEAVE,
    INACTIVE
}
