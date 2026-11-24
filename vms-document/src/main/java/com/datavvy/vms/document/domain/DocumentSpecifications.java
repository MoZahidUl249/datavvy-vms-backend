package com.datavvy.vms.document.domain;

import com.datavvy.vms.document.api.DocumentFilter;
import com.datavvy.vms.platform.time.Expiry;
import com.datavvy.vms.platform.time.ExpiryStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/** Translates {@link DocumentFilter} into a JPA predicate. Expiry states become date ranges. */
public final class DocumentSpecifications {

    private DocumentSpecifications() {
    }

    /**
     * @param vehicleIdsForQ vehicles whose number matches {@code filter.q()} (from the vehicle module)
     */
    public static Specification<VehicleDocument> matching(DocumentFilter filter, Set<Long> vehicleIdsForQ,
            LocalDate today, int soonDays) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (filter.vehicleId() != null) {
                p.add(cb.equal(root.get("vehicleId"), filter.vehicleId()));
            }
            if (filter.types() != null && !filter.types().isEmpty()) {
                p.add(root.get("type").in(filter.types()));
            }
            if (filter.expiry() != null && !filter.expiry().isEmpty()) {
                Path<LocalDate> expiry = root.get("expiryDate");
                LocalDate soon = Expiry.soonLimit(today, soonDays);
                List<Predicate> any = new ArrayList<>();
                for (ExpiryStatus s : Set.copyOf(filter.expiry())) {
                    any.add(switch (s) {
                        case EXPIRED -> cb.lessThan(expiry, today);
                        case EXPIRING_SOON -> cb.between(expiry, today, soon);
                        case VALID -> cb.greaterThan(expiry, soon);
                        case NO_EXPIRY -> cb.isNull(expiry);
                    });
                }
                p.add(cb.or(any.toArray(Predicate[]::new)));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = "%" + filter.q().trim().toLowerCase(Locale.ROOT) + "%";
                List<Predicate> any = new ArrayList<>();
                any.add(likeLower(cb, root.get("title"), like));
                any.add(likeLower(cb, root.get("documentNumber"), like));
                if (!vehicleIdsForQ.isEmpty()) {
                    any.add(root.get("vehicleId").in(vehicleIdsForQ));
                }
                p.add(cb.or(any.toArray(Predicate[]::new)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static Predicate likeLower(CriteriaBuilder cb, Path<String> path, String like) {
        return cb.like(cb.lower(path), like);
    }
}
