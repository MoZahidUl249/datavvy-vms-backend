package com.datavvy.vms.document.domain;

import com.datavvy.vms.document.api.DocumentType;
import java.time.LocalDate;

/** Validated, tidied document fields ready to apply. */
public record DocumentDetails(DocumentType type, String title, String documentNumber, LocalDate issueDate,
        LocalDate expiryDate, String notes) {
}
