package com.datavvy.vms.vehicle.api;

import java.util.List;

/** Read-only fleet queries for other modules (e.g. dashboard). */
public interface VehicleQueryApi {

    VehicleStats stats();

    List<VehicleSummary> recentlyUpdated(int limit);

    List<VehicleSummary> withStatus(VehicleStatus status, int limit);
}
