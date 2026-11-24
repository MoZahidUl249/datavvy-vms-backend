package com.datavvy.vms.driver.api;

import java.time.Instant;

/**
 * One row of assignment history. Names are snapshots taken at assignment time,
 * so history stays readable after a driver or vehicle is deleted.
 * {@code releasedAt} is null for the current assignment.
 */
public record AssignmentRecord(Long id, Long vehicleId, String vehicleNumber, Long driverId, String driverName,
        Instant assignedAt, String assignedBy, Instant releasedAt, String releasedBy) {
}
