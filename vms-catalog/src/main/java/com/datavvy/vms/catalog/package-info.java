/**
 * Catalog module: workspace-defined lists such as vehicle types
 * (Settings screen). Owned by the Fleet Settings team.
 *
 * <p>Public surface: {@code com.datavvy.vms.catalog.api}. Other modules read
 * catalog values through {@link com.datavvy.vms.catalog.api.CatalogLookup} and
 * report usage back through {@link com.datavvy.vms.catalog.api.CatalogUsageProvider}
 * (dependency inversion: catalog never depends on its consumers).
 */
package com.datavvy.vms.catalog;
