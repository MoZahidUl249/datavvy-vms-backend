package com.datavvy.vms.platform.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ExpiryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    @Test
    void noDateMeansNoExpiry() {
        assertThat(Expiry.of(null, TODAY, 30)).isEqualTo(new Expiry(ExpiryStatus.NO_EXPIRY, null));
    }

    @Test
    void yesterdayIsExpired() {
        assertThat(Expiry.of(TODAY.minusDays(1), TODAY, 30)).isEqualTo(new Expiry(ExpiryStatus.EXPIRED, -1));
    }

    @Test
    void theExpiryDayItselfIsStillValidButExpiringSoon() {
        assertThat(Expiry.of(TODAY, TODAY, 30)).isEqualTo(new Expiry(ExpiryStatus.EXPIRING_SOON, 0));
    }

    @Test
    void theLastDayOfTheWindowIsExpiringSoon() {
        assertThat(Expiry.of(TODAY.plusDays(30), TODAY, 30).status()).isEqualTo(ExpiryStatus.EXPIRING_SOON);
    }

    @Test
    void beyondTheWindowIsValid() {
        assertThat(Expiry.of(TODAY.plusDays(31), TODAY, 30)).isEqualTo(new Expiry(ExpiryStatus.VALID, 31));
    }
}
