package com.datavvy.vms.document.api;

import com.datavvy.vms.platform.time.ExpiryStatus;
import java.util.List;

/**
 * Fleet-wide document list filters; null / empty means "don't filter".
 * {@code expiry} values are OR-ed ({@code EXPIRED} + {@code EXPIRING_SOON} = "needs attention").
 * {@code q} matches the vehicle number (ignoring spaces/hyphens), title and document number.
 */
public record DocumentFilter(Long vehicleId, List<DocumentType> types, List<ExpiryStatus> expiry, String q) {
}
