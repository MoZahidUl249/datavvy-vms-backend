package com.datavvy.vms.driver.api;

import jakarta.validation.constraints.NotNull;

/**
 * Assign {@code driverId} to the vehicle in the path. If the driver is on another
 * vehicle the request fails with {@code DRIVER_ASSIGNED_ELSEWHERE} unless
 * {@code reassign} is true, which moves them here.
 */
public record AssignmentRequest(
        @NotNull(message = "Choose a driver.")
        Long driverId,
        boolean reassign) {
}
