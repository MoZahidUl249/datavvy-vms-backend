package com.datavvy.vms.vehicle.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Read-only fleet queries for other modules (dashboard, documents, maintenance, drivers). */
public interface VehicleQueryApi {

    VehicleStats stats();

    List<VehicleSummary> recentlyUpdated(int limit);

    List<VehicleSummary> withStatus(VehicleStatus status, int limit);

    /** One vehicle, for modules that hold a vehicle id (ADR-0003: ids, not foreign keys). */
    Optional<VehicleSummary> find(Long id);

    /** Many vehicles by id, for joining vehicle numbers into another module's list. Missing ids are absent. */
    Map<Long, VehicleSummary> findAll(Collection<Long> ids);

    /**
     * Ids of vehicles whose number contains {@code q}, ignoring case, spaces and
     * hyphens ("ga 11" matches DHA-METRO-GA 11-4582). Lets other modules search
     * their records by vehicle number.
     */
    Set<Long> idsWithNumberLike(String q);
}
