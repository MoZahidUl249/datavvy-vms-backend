package com.datavvy.vms.maintenance.web;

import com.datavvy.vms.maintenance.api.MaintenanceFilter;
import com.datavvy.vms.maintenance.api.MaintenanceHistorySummary;
import com.datavvy.vms.maintenance.api.MaintenanceQueryApi;
import com.datavvy.vms.maintenance.api.MaintenanceRequest;
import com.datavvy.vms.maintenance.api.MaintenanceResponse;
import com.datavvy.vms.maintenance.api.MaintenanceStats;
import com.datavvy.vms.maintenance.api.MaintenanceType;
import com.datavvy.vms.maintenance.application.MaintenanceService;
import com.datavvy.vms.platform.security.Roles;
import com.datavvy.vms.platform.web.PageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Maintenance records and history.
 *
 * <pre>
 * GET    /api/v1/vehicles/{vehicleId}/maintenance           200 page (history, newest first) | 404
 * GET    /api/v1/vehicles/{vehicleId}/maintenance/summary   200 count, total cost, last service | 404
 * POST   /api/v1/vehicles/{vehicleId}/maintenance           201 | 400 | 404
 * GET    /api/v1/maintenance                                200 page (fleet log: type, from, to, q, vehicleId)
 * GET    /api/v1/maintenance/stats                          200 fleet figures
 * GET    /api/v1/maintenance/{id}                           200 | 404
 * PUT    /api/v1/maintenance/{id}                           200 | 400 | 404 | 409 STALE_VERSION
 * DELETE /api/v1/maintenance/{id}                           204 | 404
 * </pre>
 */
@RestController
public class MaintenanceController {

    private final MaintenanceService service;
    private final MaintenanceQueryApi queries;

    public MaintenanceController(MaintenanceService service, MaintenanceQueryApi queries) {
        this.service = service;
        this.queries = queries;
    }

    @GetMapping("/api/v1/vehicles/{vehicleId}/maintenance")
    @PreAuthorize(Roles.CAN_READ)
    public PageResponse<MaintenanceResponse> history(@PathVariable Long vehicleId,
            @PageableDefault(size = 20, sort = "serviceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.history(vehicleId, pageable);
    }

    @GetMapping("/api/v1/vehicles/{vehicleId}/maintenance/summary")
    @PreAuthorize(Roles.CAN_READ)
    public MaintenanceHistorySummary historySummary(@PathVariable Long vehicleId) {
        return service.historySummary(vehicleId);
    }

    @PostMapping("/api/v1/vehicles/{vehicleId}/maintenance")
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public ResponseEntity<MaintenanceResponse> create(@PathVariable Long vehicleId,
            @Valid @RequestBody MaintenanceRequest request) {
        MaintenanceResponse created = service.create(vehicleId, request);
        return ResponseEntity.created(URI.create("/api/v1/maintenance/" + created.id())).body(created);
    }

    @GetMapping("/api/v1/maintenance")
    @PreAuthorize(Roles.CAN_READ)
    public PageResponse<MaintenanceResponse> search(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(name = "type", required = false) List<MaintenanceType> types,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "serviceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(new MaintenanceFilter(vehicleId, types, from, to, q), pageable);
    }

    @GetMapping("/api/v1/maintenance/stats")
    @PreAuthorize(Roles.CAN_READ)
    public MaintenanceStats stats() {
        return queries.stats();
    }

    @GetMapping("/api/v1/maintenance/{id}")
    @PreAuthorize(Roles.CAN_READ)
    public MaintenanceResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/api/v1/maintenance/{id}")
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public MaintenanceResponse update(@PathVariable Long id, @Valid @RequestBody MaintenanceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/v1/maintenance/{id}")
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
