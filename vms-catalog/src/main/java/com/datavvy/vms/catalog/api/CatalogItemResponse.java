package com.datavvy.vms.catalog.api;

public record CatalogItemResponse(Long id, String kind, String name, int sortOrder, long usageCount) {
}
