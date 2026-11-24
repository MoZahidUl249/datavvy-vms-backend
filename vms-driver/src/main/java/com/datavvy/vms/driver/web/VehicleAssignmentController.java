package com.datavvy.vms.driver.web;

import com.datavvy.vms.driver.api.AssignmentRecord;
import com.datavvy.vms.driver.api.AssignmentRequest;
import com.datavvy.vms.driver.api.VehicleAssignment;
import com.datavvy.vms.driver.application.AssignmentService;
import com.datavvy.vms.platform.security.Roles;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A vehicle's driver.
 *
 * <pre>
 * GET    /api/v1/vehicles/{vehicleId}/assignment           200 current driver (driver null if none) | 404
 * PUT    /api/v1/vehicles/{vehicleId}/assignment           200 | 400 | 404 | 409 DRIVER_ASSIGNED_ELSEWHERE
 *                                                          | 422 VEHICLE_INACTIVE / DRIVER_NOT_ACTIVE / DRIVER_LICENSE_EXPIRED
 * DELETE /api/v1/vehicles/{vehicleId}/assignment           204 | 404
 * GET    /api/v1/vehicles/{vehicleId}/assignment/history   200 newest first | 404
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/assignment")
public class VehicleAssignmentController {

    private final AssignmentService service;

    public VehicleAssignmentController(AssignmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(Roles.CAN_READ)
    public VehicleAssignment current(@PathVariable Long vehicleId) {
        return service.current(vehicleId);
    }

    @PutMapping
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public VehicleAssignment assign(@PathVariable Long vehicleId, @Valid @RequestBody AssignmentRequest request) {
        return service.assign(vehicleId, request);
    }

    @DeleteMapping
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public ResponseEntity<Void> unassign(@PathVariable Long vehicleId) {
        service.unassign(vehicleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    @PreAuthorize(Roles.CAN_READ)
    public List<AssignmentRecord> history(@PathVariable Long vehicleId) {
        return service.history(vehicleId);
    }
}
