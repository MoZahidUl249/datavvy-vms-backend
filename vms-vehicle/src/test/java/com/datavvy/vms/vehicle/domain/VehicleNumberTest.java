package com.datavvy.vms.vehicle.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VehicleNumberTest {

    @ParameterizedTest
    @CsvSource({
        "DHA-METRO-GA 11-4582, DHA-METRO-GA 11-4582",
        "dha metro ga 11 4582, DHA-METRO-GA 11-4582",
        "DHAMETROGA114582,     DHA-METRO-GA 11-4582",
        "syl-ga 11-0458,       SYL-GA 11-0458",
        "DHA-METRO-KHA 14-2210, DHA-METRO-KHA 14-2210",
    })
    void canonicalisesValidNumbers(String raw, String expected) {
        assertThat(VehicleNumber.canonical(raw)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "DHA", "DHA-GA 1-4582", "DHA-GA 11-458", "DH-GA 11-4582", "DHA-ABCD 11-4582"})
    void rejectsInvalidNumbers(String raw) {
        assertThat(VehicleNumber.canonical(raw)).isEmpty();
    }

    @Test
    void keyIgnoresSpacesHyphensAndCase() {
        assertThat(VehicleNumber.key("dha-metro-ga 11-4582")).isEqualTo("DHAMETROGA114582");
        assertThat(VehicleNumber.key("DHA METRO GA 11 4582")).isEqualTo(VehicleNumber.key("DHA-METRO-GA 11-4582"));
    }
}
