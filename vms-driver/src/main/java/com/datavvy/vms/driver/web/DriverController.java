package com.datavvy.vms.driver.web;

import com.datavvy.vms.driver.api.AssignmentRecord;
import com.datavvy.vms.driver.api.DriverFilter;
import com.datavvy.vms.driver.api.DriverQueryApi;
import com.datavvy.vms.driver.api.DriverRequest;
import com.datavvy.vms.driver.api.DriverResponse;
import com.datavvy.vms.driver.api.DriverStats;
import com.datavvy.vms.driver.api.DriverStatus;
import com.datavvy.vms.driver.application.DriverService;
import com.datavvy.vms.platform.security.Roles;
import com.datavvy.vms.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
 * Driver CRUD.
 *
 * <pre>
 * GET    /api/v1/drivers                    200 page (q, status, assigned, sort)
 * GET    /api/v1/drivers/stats              200 counts
 * POST   /api/v1/drivers                    201 | 400 | 409 DUPLICATE_LICENSE_NUMBER
 * GET    /api/v1/drivers/{id}               200 | 404
 * PUT    /api/v1/drivers/{id}               200 | 400 | 404 | 409 DUPLICATE_LICENSE_NUMBER / STALE_VERSION
 * DELETE /api/v1/drivers/{id}               204 | 404 | 409 DRIVER_ASSIGNED
 * GET    /api/v1/drivers/{id}/assignments   200 vehicles this driver has driven, newest first
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService service;
    private final DriverQueryApi queries;

    public DriverController(DriverService service, DriverQueryApi queries) {
        this.service = service;
        this.queries = queries;
    }

    @GetMapping
    @PreAuthorize(Roles.CAN_READ)
    public PageResponse<DriverResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(name = "status", required = false) List<DriverStatus> statuses,
            @RequestParam(required = false) Boolean assigned,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(new DriverFilter(q, statuses, assigned), pageable);
    }

    @GetMapping("/stats")
    @PreAuthorize(Roles.CAN_READ)
    public DriverStats stats() {
        return queries.stats();
    }

    @PostMapping
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody DriverRequest request) {
        DriverResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/drivers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.CAN_READ)
    public DriverResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public DriverResponse update(@PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize(Roles.CAN_READ)
    public List<AssignmentRecord> assignments(@PathVariable Long id) {
        return service.assignmentHistory(id);
    }
}
