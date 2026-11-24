package com.datavvy.vms.driver.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneAndLicenceTest {

    @Test
    void acceptsTheUsualWaysOfWritingABangladeshiMobile() {
        assertThat(PhoneNumber.canonical("01712345678")).contains("01712345678");
        assertThat(PhoneNumber.canonical("+880 1712-345678")).contains("01712345678");
        assertThat(PhoneNumber.canonical("8801712345678")).contains("01712345678");
        assertThat(PhoneNumber.canonical("(017) 1234 5678")).contains("01712345678");
    }

    @Test
    void rejectsLandlinesAndWrongLengths() {
        assertThat(PhoneNumber.canonical("0212345678")).isEmpty();
        assertThat(PhoneNumber.canonical("0171234567")).isEmpty();
        assertThat(PhoneNumber.canonical("01212345678")).isEmpty();
        assertThat(PhoneNumber.canonical(null)).isEmpty();
    }

    @Test
    void licenceNumbersCompareIgnoringCaseSpacesHyphensAndSlashes() {
        assertThat(LicenseNumber.key("dk-0123456 c/00001")).isEqualTo("DK0123456C00001");
        assertThat(LicenseNumber.display("  dk 0123456   c00001 ")).isEqualTo("DK 0123456 C00001");
        assertThat(LicenseNumber.looksValid("DK0123456C00001")).isTrue();
        assertThat(LicenseNumber.looksValid("DK#1")).isFalse();
    }
}
