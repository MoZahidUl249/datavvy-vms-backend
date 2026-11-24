package com.datavvy.vms.document.api;

import com.datavvy.vms.platform.time.ExpiryStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A vehicle document with its expiry state worked out for today (business time
 * zone) and the stored file's metadata. The file itself is at
 * {@code GET /api/v1/documents/{id}/file}.
 */
public record DocumentResponse(
        Long id,
        Long vehicleId,
        String vehicleNumber,
        DocumentType type,
        String title,
        String documentNumber,
        LocalDate issueDate,
        LocalDate expiryDate,
        ExpiryStatus expiryStatus,
        Integer daysUntilExpiry,
        String notes,
        String fileName,
        String contentType,
        long sizeBytes,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy,
        long version) {
}
