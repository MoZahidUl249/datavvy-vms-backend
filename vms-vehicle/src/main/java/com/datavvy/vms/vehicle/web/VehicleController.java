package com.datavvy.vms.vehicle.web;

import com.datavvy.vms.platform.security.Roles;
import com.datavvy.vms.platform.web.PageResponse;
import com.datavvy.vms.vehicle.api.VehicleFacets;
import com.datavvy.vms.vehicle.api.VehicleFilter;
import com.datavvy.vms.vehicle.api.VehicleNumberCheckResponse;
import com.datavvy.vms.vehicle.api.VehicleRequest;
import com.datavvy.vms.vehicle.api.VehicleResponse;
import com.datavvy.vms.vehicle.api.VehicleStatus;
import com.datavvy.vms.vehicle.application.VehicleService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vehicle CRUD. Thin by design: HTTP ⇄ DTOs only; every rule (including the
 * duplicate Vehicle Number rule) is in {@link VehicleService}.
 *
 * <pre>
 * POST   /api/v1/vehicles        201 + Location | 400 | 403 | 409 DUPLICATE_VEHICLE_NUMBER
 * GET    /api/v1/vehicles        200 page
 * GET    /api/v1/vehicles/{id}   200 | 404
 * PUT    /api/v1/vehicles/{id}   200 | 400 | 404 | 409 DUPLICATE_VEHICLE_NUMBER / STALE_VERSION
 * DELETE /api/v1/vehicles/{id}   204 | 404
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        VehicleResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/vehicles/" + created.id())).body(created);
    }

    /**
     * {@code GET /api/v1/vehicles?status=ACTIVE&q=hiace&type=Microbus&company=Datavvy%20Logistics
     * &brand=Toyota&updatedOn=2026-11-19&addedOn=2026-11-12&page=0&size=20&sort=updatedAt,desc}
     */
    @GetMapping
    @PreAuthorize(Roles.CAN_READ)
    public PageResponse<VehicleResponse> list(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(name = "type", required = false) List<String> types,
            @RequestParam(name = "company", required = false) List<String> companies,
            @RequestParam(name = "brand", required = false) List<String> brands,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedOn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate addedOn,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(new VehicleFilter(status, q, types, companies, brands, updatedOn, addedOn), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.CAN_READ)
    public VehicleResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -- helpers for the UI (read-only)

    @GetMapping("/facets")
    @PreAuthorize(Roles.CAN_READ)
    public VehicleFacets facets() {
        return service.facets();
    }

    @GetMapping("/number-check")
    @PreAuthorize(Roles.CAN_READ)
    public VehicleNumberCheckResponse checkNumber(@RequestParam String number,
            @RequestParam(required = false) Long excludeId) {
        return service.checkVehicleNumber(number, excludeId);
    }
}
