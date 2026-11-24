package com.datavvy.vms.document.api;

/** Fleet-wide document counts by expiry state; {@code expiringSoonDays} is the window used. */
public record DocumentStats(long total, long valid, long expiringSoon, long expired, long noExpiry,
        int expiringSoonDays) {
}
