package com.datavvy.vms.platform.time;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The expiry rule shared by vehicle documents and driving licences, so both
 * screens agree on what "expiring soon" means.
 *
 * <ul>
 *   <li>no expiry date → {@code NO_EXPIRY}</li>
 *   <li>expiry before today → {@code EXPIRED} (the expiry day itself is still valid)</li>
 *   <li>expiry within {@code soonDays} days → {@code EXPIRING_SOON}</li>
 *   <li>otherwise → {@code VALID}</li>
 * </ul>
 *
 * @param status   the classification
 * @param daysLeft days from today to the expiry date (negative once expired), null without a date
 */
public record Expiry(ExpiryStatus status, Integer daysLeft) {

    public static Expiry of(LocalDate expiryDate, LocalDate today, int soonDays) {
        if (expiryDate == null) {
            return new Expiry(ExpiryStatus.NO_EXPIRY, null);
        }
        int days = (int) ChronoUnit.DAYS.between(today, expiryDate);
        if (days < 0) {
            return new Expiry(ExpiryStatus.EXPIRED, days);
        }
        return new Expiry(days <= soonDays ? ExpiryStatus.EXPIRING_SOON : ExpiryStatus.VALID, days);
    }

    /** Last expiry date that still counts as "expiring soon" (inclusive). */
    public static LocalDate soonLimit(LocalDate today, int soonDays) {
        return today.plusDays(soonDays);
    }
}
