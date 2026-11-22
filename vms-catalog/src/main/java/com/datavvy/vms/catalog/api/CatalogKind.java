package com.datavvy.vms.catalog.api;

import java.util.Arrays;
import java.util.Optional;

/** The lists the catalog manages. Add a constant (and a seed migration) to add a list. */
public enum CatalogKind {
    VEHICLE_TYPE("vehicle-types", "Vehicle type");

    private final String slug;
    private final String label;

    CatalogKind(String slug, String label) {
        this.slug = slug;
        this.label = label;
    }

    /** URL segment, e.g. {@code /api/v1/catalog/vehicle-types}. */
    public String slug() {
        return slug;
    }

    public String label() {
        return label;
    }

    public static Optional<CatalogKind> fromSlug(String slug) {
        return Arrays.stream(values()).filter(k -> k.slug.equals(slug)).findFirst();
    }
}
