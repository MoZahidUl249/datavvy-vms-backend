package com.datavvy.vms.maintenance.api;

import java.util.List;

/** Read-only maintenance queries for other modules (dashboard). */
public interface MaintenanceQueryApi {

    MaintenanceStats stats();

    /** Latest records by service date. */
    List<MaintenanceResponse> recent(int limit);
}
