package com.datavvy.vms.document.api;

import com.datavvy.vms.platform.time.ExpiryStatus;
import java.time.LocalDate;

/** A document that is expired or expiring soon — a dashboard to-do. */
public record DocumentAlert(Long id, Long vehicleId, String vehicleNumber, DocumentType type, String title,
        LocalDate expiryDate, ExpiryStatus expiryStatus, Integer daysUntilExpiry) {
}
