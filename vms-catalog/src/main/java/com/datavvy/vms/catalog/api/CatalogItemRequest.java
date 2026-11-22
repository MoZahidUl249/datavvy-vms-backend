package com.datavvy.vms.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogItemRequest(
        @NotBlank(message = "Enter a name, like Microbus.")
        @Size(max = 60, message = "Keep the name under 60 characters.")
        String name) {
}
