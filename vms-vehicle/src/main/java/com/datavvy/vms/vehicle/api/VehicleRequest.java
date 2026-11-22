package com.datavvy.vms.vehicle.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload — the course fields: Vehicle Number, Company, Type,
 * Brand, Model, Status. {@code vehicleNumber} is accepted loosely
 * ("dha metro ga 11 4582") and stored canonically ("DHA-METRO-GA 11-4582").
 * {@code version} is required on update (optimistic locking) and ignored on create.
 */
public record VehicleRequest(
        @NotBlank(message = "Enter the vehicle number, like DHA-METRO-GA 11-4582.")
        @Size(max = 32, message = "Vehicle number is too long; use the format DHA-METRO-GA 11-4582.")
        String vehicleNumber,

        @NotBlank(message = "Enter the company, like Datavvy Logistics.")
        @Size(max = 100, message = "Keep the company under 100 characters.")
        String company,

        @NotBlank(message = "Choose a vehicle type, like Microbus.")
        @Size(max = 60, message = "Keep the type under 60 characters.")
        String type,

        @NotBlank(message = "Enter the brand, like Toyota.")
        @Size(max = 60, message = "Keep the brand under 60 characters.")
        String brand,

        @NotBlank(message = "Enter the model, like Hiace.")
        @Size(max = 60, message = "Keep the model under 60 characters.")
        String model,

        @NotNull(message = "Choose a status: ACTIVE, INACTIVE or MAINTENANCE.")
        VehicleStatus status,

        Long version) {
}
