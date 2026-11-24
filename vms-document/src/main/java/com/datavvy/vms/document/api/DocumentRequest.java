package com.datavvy.vms.document.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Document details. Sent as JSON to update a document, or as multipart form
 * fields (next to {@code file}) to upload one. {@code version} is required on
 * update and ignored on upload.
 */
public record DocumentRequest(
        @NotNull(message = "Choose the document type, like INSURANCE.")
        DocumentType type,

        @Size(max = 120, message = "Keep the title under 120 characters.")
        String title,

        @Size(max = 60, message = "Keep the document number under 60 characters.")
        String documentNumber,

        LocalDate issueDate,

        LocalDate expiryDate,

        @Size(max = 500, message = "Keep the notes under 500 characters.")
        String notes,

        Long version) {
}
