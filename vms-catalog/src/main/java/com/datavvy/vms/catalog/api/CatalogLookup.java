package com.datavvy.vms.catalog.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-only catalog access for other modules. */
public interface CatalogLookup {

    Optional<CatalogItemRef> find(CatalogKind kind, Long id);

    /** Case-insensitive lookup by name; returns the stored spelling. */
    Optional<CatalogItemRef> findByName(CatalogKind kind, String name);

    /** id → name for every value of the list (small lists; safe to load whole). */
    Map<Long, String> names(CatalogKind kind);

    List<CatalogItemRef> list(CatalogKind kind);
}
