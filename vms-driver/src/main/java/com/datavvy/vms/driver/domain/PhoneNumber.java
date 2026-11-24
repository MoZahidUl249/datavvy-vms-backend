package com.datavvy.vms.driver.domain;

import java.util.Optional;
import java.util.regex.Pattern;

/** Bangladeshi mobile numbers: 01[3-9] + 8 digits, stored as {@code 01XXXXXXXXX}. */
public final class PhoneNumber {

    private static final Pattern MOBILE = Pattern.compile("^01[3-9]\\d{8}$");

    private PhoneNumber() {
    }

    /** "+880 1712-345678", "8801712345678" and "01712 345678" all become "01712345678". */
    public static Optional<String> canonical(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("[\\s\\-().]", "");
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        if (digits.startsWith("880")) {
            digits = digits.substring(2);
        }
        return MOBILE.matcher(digits).matches() ? Optional.of(digits) : Optional.empty();
    }
}
