package com.datavvy.vms.driver.domain;

import java.util.Locale;

/** Driving licence numbers, compared ignoring case, spaces, hyphens and slashes. */
public final class LicenseNumber {

    private LicenseNumber() {
    }

    /** Display form: uppercase, single spaces. */
    public static String display(String raw) {
        return raw == null ? null : raw.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    /** Uniqueness key stored under a UNIQUE constraint, e.g. "dk-0123456 c00001" → "DK0123456C00001". */
    public static String key(String raw) {
        return raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[\\s\\-/]", "");
    }

    public static boolean looksValid(String raw) {
        return key(raw).matches("[A-Z0-9]{5,30}");
    }
}
