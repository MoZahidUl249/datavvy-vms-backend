package com.datavvy.vms.driver.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Create / update a driver. {@code phone} accepts 01XXXXXXXXX, +8801XXXXXXXXX or
 * with spaces/hyphens and is stored as 01XXXXXXXXX. {@code licenseNumber} is unique
 * ignoring case, spaces, hyphens and slashes. {@code version} is required on update.
 */
public record DriverRequest(
        @NotBlank(message = "Enter the driver's full name, like Md. Rahim Uddin.")
        @Size(max = 100, message = "Keep the name under 100 characters.")
        String fullName,

        @NotBlank(message = "Enter a mobile number, like 01712345678.")
        @Size(max = 20, message = "Enter a mobile number, like 01712345678.")
        String phone,

        @NotBlank(message = "Enter the driving licence number, like DK0123456C00001.")
        @Size(max = 30, message = "Keep the licence number under 30 characters.")
        String licenseNumber,

        @NotNull(message = "Enter the licence expiry date, like 2028-03-31.")
        LocalDate licenseExpiry,

        @Size(max = 200, message = "Keep the address under 200 characters.")
        String address,

        LocalDate joinedOn,

        @NotNull(message = "Choose a status: ACTIVE, ON_LEAVE or INACTIVE.")
        DriverStatus status,

        @Size(max = 500, message = "Keep the notes under 500 characters.")
        String notes,

        Long version) {
}
