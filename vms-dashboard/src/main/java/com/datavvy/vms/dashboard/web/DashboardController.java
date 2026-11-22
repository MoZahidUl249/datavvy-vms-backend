package com.datavvy.vms.dashboard.web;

import com.datavvy.vms.dashboard.api.DashboardSummary;
import com.datavvy.vms.dashboard.application.DashboardService;
import com.datavvy.vms.platform.security.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/summary")
    @PreAuthorize(Roles.CAN_READ)
    public DashboardSummary summary() {
        return dashboard.summary();
    }
}
