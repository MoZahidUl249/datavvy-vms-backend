package com.datavvy.vms.platform.time;

/** Where a dated permit (vehicle document, driving licence) stands today. */
public enum ExpiryStatus {
    EXPIRED,
    EXPIRING_SOON,
    VALID,
    NO_EXPIRY
}
