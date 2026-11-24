package com.datavvy.vms.maintenance.api;

import java.time.LocalDate;
import java.util.List;

/** Fleet-wide maintenance log filters; null / empty = no filter. {@code q} matches vehicle number, vendor and notes. */
public record MaintenanceFilter(Long vehicleId, List<MaintenanceType> types, LocalDate from, LocalDate to, String q) {
}
