package com.datavvy.vms.maintenance.domain;

import com.datavvy.vms.maintenance.api.MaintenanceFilter;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/** Translates {@link MaintenanceFilter} into a JPA predicate. */
public final class MaintenanceSpecifications {

    private MaintenanceSpecifications() {
    }

    /** @param vehicleIdsForQ vehicles whose number matches {@code filter.q()} (from the vehicle module) */
    public static Specification<MaintenanceRecord> matching(MaintenanceFilter f, Set<Long> vehicleIdsForQ) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.vehicleId() != null) {
                p.add(cb.equal(root.get("vehicleId"), f.vehicleId()));
            }
            if (f.types() != null && !f.types().isEmpty()) {
                p.add(root.get("type").in(f.types()));
            }
            if (f.from() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("serviceDate"), f.from()));
            }
            if (f.to() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("serviceDate"), f.to()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(root.get("vendor")), like));
                any.add(cb.like(cb.lower(root.get("notes")), like));
                if (!vehicleIdsForQ.isEmpty()) {
                    any.add(root.get("vehicleId").in(vehicleIdsForQ));
                }
                p.add(cb.or(any.toArray(Predicate[]::new)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
