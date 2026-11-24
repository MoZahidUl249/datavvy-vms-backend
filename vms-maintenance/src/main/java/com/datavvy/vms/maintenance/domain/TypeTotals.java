package com.datavvy.vms.maintenance.domain;

import com.datavvy.vms.maintenance.api.MaintenanceType;
import java.math.BigDecimal;

public record TypeTotals(MaintenanceType type, Long count, BigDecimal cost) {
}
