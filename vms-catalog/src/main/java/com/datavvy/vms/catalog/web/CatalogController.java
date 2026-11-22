package com.datavvy.vms.catalog.web;

import com.datavvy.vms.catalog.api.CatalogItemRequest;
import com.datavvy.vms.catalog.api.CatalogItemResponse;
import com.datavvy.vms.catalog.api.CatalogKind;
import com.datavvy.vms.catalog.application.CatalogService;
import com.datavvy.vms.platform.error.NotFoundException;
import com.datavvy.vms.platform.security.Roles;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/catalog/{kind}} — kind is {@code vehicle-types} (more lists can be added to CatalogKind). */
@RestController
@RequestMapping("/api/v1/catalog/{kind}")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @PreAuthorize(Roles.CAN_READ)
    public List<CatalogItemResponse> list(@PathVariable String kind) {
        return catalog.listWithUsage(kind(kind));
    }

    @PostMapping
    @PreAuthorize(Roles.CAN_ADMINISTER)
    public ResponseEntity<CatalogItemResponse> create(@PathVariable String kind,
            @Valid @RequestBody CatalogItemRequest request) {
        CatalogItemResponse created = catalog.create(kind(kind), request.name());
        return ResponseEntity.created(URI.create("/api/v1/catalog/" + kind + "/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(Roles.CAN_ADMINISTER)
    public CatalogItemResponse rename(@PathVariable String kind, @PathVariable Long id,
            @Valid @RequestBody CatalogItemRequest request) {
        return catalog.rename(kind(kind), id, request.name());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.CAN_ADMINISTER)
    public ResponseEntity<Void> delete(@PathVariable String kind, @PathVariable Long id) {
        catalog.delete(kind(kind), id);
        return ResponseEntity.noContent().build();
    }

    private static CatalogKind kind(String slug) {
        return CatalogKind.fromSlug(slug).orElseThrow(() -> new NotFoundException("Catalog", slug));
    }
}
