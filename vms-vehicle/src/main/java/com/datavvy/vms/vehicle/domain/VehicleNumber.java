package com.datavvy.vms.vehicle.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bangladesh vehicle number: Area · [METRO] · Class · Series · Number,
 * canonical form {@code DHA-METRO-GA 11-4582}. Mirrors the client plate input
 * ({@code feature_vehicles/lib/src/domain/plate.dart}); keep both in sync.
 */
public final class VehicleNumber {

    private static final Pattern LOOSE = Pattern.compile(
            "^([A-Z]{3})(?:[\\s-]*(METRO))?[\\s-]*([A-Z]{1,3})[\\s-]*(\\d{2})[\\s-]*(\\d{4})$");

    private VehicleNumber() {
    }

    /** Parses any reasonable spelling and returns the canonical form, or empty if invalid. */
    public static Optional<String> canonical(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String tidy = raw.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        Matcher m = LOOSE.matcher(tidy);
        if (!m.matches()) {
            return Optional.empty();
        }
        String metro = m.group(2) != null ? "-METRO" : "";
        return Optional.of(m.group(1) + metro + "-" + m.group(3) + " " + m.group(4) + "-" + m.group(5));
    }

    /**
     * Uniqueness key: uppercase with spaces and hyphens removed, so
     * "dha metro ga 11 4582" and "DHA-METRO-GA 11-4582" count as the same vehicle.
     * Stored in {@code vehicle.vehicle_number_key} under a UNIQUE constraint.
     */
    public static String key(String raw) {
        return raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
    }
}
