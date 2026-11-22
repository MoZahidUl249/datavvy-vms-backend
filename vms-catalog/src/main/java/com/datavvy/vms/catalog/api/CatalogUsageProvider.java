package com.datavvy.vms.catalog.api;

import java.util.Map;

/**
 * SPI implemented by modules that reference catalog values (e.g. vehicle).
 * The catalog sums all providers to show usage counts and to block deleting a
 * value that is still in use.
 */
public interface CatalogUsageProvider {

    /** itemId → number of records using it, for one list. Missing ids mean zero. */
    Map<Long, Long> usageCounts(CatalogKind kind);
}
