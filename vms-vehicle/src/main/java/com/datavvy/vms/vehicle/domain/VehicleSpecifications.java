package com.datavvy.vms.vehicle.domain;

import com.datavvy.vms.vehicle.api.VehicleFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Translates {@link VehicleFilter} into a JPA criteria predicate. */
public final class VehicleSpecifications {

    private VehicleSpecifications() {
    }

    public static Specification<Vehicle> matching(VehicleFilter f, ZoneId zone) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            if (f.status() != null) {
                p.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                String keyLike = "%" + VehicleNumber.key(f.q()) + "%";
                p.add(cb.or(
                        cb.like(root.get("vehicleNumberKey"), keyLike),
                        cb.like(cb.lower(root.get("company")), like),
                        cb.like(cb.lower(root.get("brand")), like),
                        cb.like(cb.lower(root.get("model")), like),
                        cb.like(cb.lower(root.get("type")), like)));
            }
            inIgnoreCase(cb, root.get("type"), f.types(), p);
            inIgnoreCase(cb, root.get("company"), f.companies(), p);
            inIgnoreCase(cb, root.get("brand"), f.brands(), p);
            if (f.updatedOn() != null) {
                p.add(onDay(cb, root.get("updatedAt"), f.updatedOn(), zone));
            }
            if (f.addedOn() != null) {
                p.add(onDay(cb, root.get("createdAt"), f.addedOn(), zone));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static void inIgnoreCase(CriteriaBuilder cb, Path<String> path, List<String> values, List<Predicate> p) {
        if (values != null && !values.isEmpty()) {
            p.add(cb.lower(path).in(values.stream().map(v -> v.toLowerCase(Locale.ROOT)).toList()));
        }
    }

    private static Predicate onDay(CriteriaBuilder cb, Path<Instant> path, LocalDate day, ZoneId zone) {
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        return cb.and(cb.greaterThanOrEqualTo(path, from), cb.lessThan(path, to));
    }
}
