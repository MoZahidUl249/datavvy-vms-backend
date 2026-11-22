package com.datavvy.vms.vehicle.api;

import java.util.Map;

/** Fleet aggregates. Maps are value → vehicle count. */
public record VehicleStats(long total, long active, long inactive, long maintenance,
        Map<String, Long> byType, Map<String, Long> byBrand) {
}
