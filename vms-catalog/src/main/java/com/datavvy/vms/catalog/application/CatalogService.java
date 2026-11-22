package com.datavvy.vms.catalog.application;

import com.datavvy.vms.catalog.api.CatalogItemRef;
import com.datavvy.vms.catalog.api.CatalogItemResponse;
import com.datavvy.vms.catalog.api.CatalogKind;
import com.datavvy.vms.catalog.api.CatalogLookup;
import com.datavvy.vms.catalog.api.CatalogUsageProvider;
import com.datavvy.vms.catalog.domain.CatalogItem;
import com.datavvy.vms.catalog.domain.CatalogItemRepository;
import com.datavvy.vms.platform.error.BusinessRuleException;
import com.datavvy.vms.platform.error.ConflictException;
import com.datavvy.vms.platform.error.NotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rules (from the Settings design): names are unique per list ignoring case,
 * a value in use can't be removed, and every list keeps at least one value.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService implements CatalogLookup {

    private final CatalogItemRepository items;
    private final ObjectProvider<CatalogUsageProvider> usageProviders;

    public CatalogService(CatalogItemRepository items, ObjectProvider<CatalogUsageProvider> usageProviders) {
        this.items = items;
        this.usageProviders = usageProviders;
    }

    public List<CatalogItemResponse> listWithUsage(CatalogKind kind) {
        Map<Long, Long> usage = usage(kind);
        return items.findByKindOrderBySortOrderAscNameAsc(kind).stream()
                .map(i -> toResponse(i, usage.getOrDefault(i.getId(), 0L)))
                .toList();
    }

    @Transactional
    public CatalogItemResponse create(CatalogKind kind, String rawName) {
        String name = tidy(rawName);
        if (items.existsByKindAndNameIgnoreCase(kind, name)) {
            throw duplicate(kind, name);
        }
        CatalogItem saved = items.save(new CatalogItem(kind, name, items.maxSortOrder(kind) + 1));
        return toResponse(saved, 0);
    }

    @Transactional
    public CatalogItemResponse rename(CatalogKind kind, Long id, String rawName) {
        CatalogItem item = load(kind, id);
        String name = tidy(rawName);
        if (items.existsByKindAndNameIgnoreCaseAndIdNot(kind, name, id)) {
            throw duplicate(kind, name);
        }
        long used = usage(kind).getOrDefault(id, 0L);
        if (used > 0 && !item.getName().equals(name)) {
            throw new ConflictException("CATALOG_ITEM_IN_USE",
                    item.getName() + " is used by " + used + " vehicle(s), so it can't be renamed. "
                            + "Add a new type instead.",
                    Map.of("usageCount", used));
        }
        item.rename(name);
        return toResponse(item, used);
    }

    @Transactional
    public void delete(CatalogKind kind, Long id) {
        CatalogItem item = load(kind, id);
        long used = usage(kind).getOrDefault(id, 0L);
        if (used > 0) {
            throw new ConflictException("CATALOG_ITEM_IN_USE",
                    item.getName() + " is used by " + used + " record(s). Change those first, then remove it.",
                    Map.of("usageCount", used));
        }
        if (items.countByKind(kind) <= 1) {
            throw new BusinessRuleException("CATALOG_KEEP_ONE",
                    "Keep at least one " + kind.label().toLowerCase() + ".");
        }
        items.delete(item);
    }

    // ---- CatalogLookup (used by other modules) ----

    @Override
    public Optional<CatalogItemRef> find(CatalogKind kind, Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return items.findByIdAndKind(id, kind).map(i -> new CatalogItemRef(i.getId(), i.getName()));
    }

    @Override
    public Optional<CatalogItemRef> findByName(CatalogKind kind, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return items.findFirstByKindAndNameIgnoreCase(kind, tidy(name))
                .map(i -> new CatalogItemRef(i.getId(), i.getName()));
    }

    @Override
    public Map<Long, String> names(CatalogKind kind) {
        Map<Long, String> names = new LinkedHashMap<>();
        items.findByKindOrderBySortOrderAscNameAsc(kind).forEach(i -> names.put(i.getId(), i.getName()));
        return names;
    }

    @Override
    public List<CatalogItemRef> list(CatalogKind kind) {
        return items.findByKindOrderBySortOrderAscNameAsc(kind).stream()
                .map(i -> new CatalogItemRef(i.getId(), i.getName()))
                .toList();
    }

    // ---- helpers ----

    private CatalogItem load(CatalogKind kind, Long id) {
        return items.findByIdAndKind(id, kind).orElseThrow(() -> new NotFoundException(kind.label(), id));
    }

    private Map<Long, Long> usage(CatalogKind kind) {
        Map<Long, Long> total = new HashMap<>();
        usageProviders.orderedStream()
                .forEach(p -> p.usageCounts(kind).forEach((id, n) -> total.merge(id, n, Long::sum)));
        return total;
    }

    private static String tidy(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ");
    }

    private static ConflictException duplicate(CatalogKind kind, String name) {
        return new ConflictException("CATALOG_DUPLICATE_NAME",
                name + " is already in the " + kind.label().toLowerCase() + " list.",
                Map.of("name", name));
    }

    private static CatalogItemResponse toResponse(CatalogItem i, long usage) {
        return new CatalogItemResponse(i.getId(), i.getKind().slug(), i.getName(), i.getSortOrder(), usage);
    }
}
