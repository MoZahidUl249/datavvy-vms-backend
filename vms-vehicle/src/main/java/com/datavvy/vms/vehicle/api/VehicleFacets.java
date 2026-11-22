package com.datavvy.vms.vehicle.api;

import java.time.LocalDate;
import java.util.List;

/**
 * What the list toolbar and filter panel need: counts for the status control,
 * the companies and brands to offer, and the dates that have data (the
 * calendar enables only those days).
 */
public record VehicleFacets(
        StatusCounts statusCounts,
        List<String> companies,
        List<String> brands,
        List<LocalDate> updatedDates,
        List<LocalDate> addedDates) {

    public record StatusCounts(long all, long active, long inactive, long maintenance) {
    }
}
