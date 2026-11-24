package com.datavvy.vms.document.api;

import java.util.List;

/** Read-only document queries for other modules (dashboard). */
public interface DocumentQueryApi {

    DocumentStats stats();

    /** Expired and expiring-soon documents, soonest expiry first. */
    List<DocumentAlert> needingAttention(int limit);
}
