package com.datavvy.vms.catalog.api;

/** Minimal reference to a catalog value, for embedding in other modules' responses. */
public record CatalogItemRef(Long id, String name) {
}
