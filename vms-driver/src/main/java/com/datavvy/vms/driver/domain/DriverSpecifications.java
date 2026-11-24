package com.datavvy.vms.driver.domain;

import com.datavvy.vms.driver.api.DriverFilter;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Translates {@link DriverFilter} into a JPA predicate. */
public final class DriverSpecifications {

    private DriverSpecifications() {
    }

    public static Specification<Driver> matching(DriverFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                String digits = f.q().replaceAll("\\D", "");
                String key = LicenseNumber.key(f.q());
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(root.get("fullName")), like));
                if (!key.isEmpty()) {
                    any.add(cb.like(root.get("licenseNumberKey"), "%" + key + "%"));
                }
                if (digits.length() >= 3) {
                    any.add(cb.like(root.get("phone"), "%" + digits + "%"));
                }
                p.add(cb.or(any.toArray(Predicate[]::new)));
            }
            if (f.statuses() != null && !f.statuses().isEmpty()) {
                p.add(root.get("status").in(f.statuses()));
            }
            if (f.assigned() != null) {
                Subquery<Long> open = query.subquery(Long.class);
                Root<DriverAssignment> a = open.from(DriverAssignment.class);
                open.select(a.get("id")).where(cb.equal(a.get("openDriverId"), root.get("id")));
                p.add(f.assigned() ? cb.exists(open) : cb.not(cb.exists(open)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
