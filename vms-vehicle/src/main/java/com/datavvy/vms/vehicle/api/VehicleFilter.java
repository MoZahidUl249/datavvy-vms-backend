package com.datavvy.vms.vehicle.api;

import java.time.LocalDate;
import java.util.List;

/** All list filters; null / empty means "don't filter on this". */
public record VehicleFilter(
        VehicleStatus status,
        String q,
        List<String> types,
        List<String> companies,
        List<String> brands,
        LocalDate updatedOn,
        LocalDate addedOn) {

    public static VehicleFilter none() {
        return new VehicleFilter(null, null, List.of(), List.of(), List.of(), null, null);
    }
}
