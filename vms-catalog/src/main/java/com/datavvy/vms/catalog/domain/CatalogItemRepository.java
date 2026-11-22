package com.datavvy.vms.catalog.domain;

import com.datavvy.vms.catalog.api.CatalogKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {

    List<CatalogItem> findByKindOrderBySortOrderAscNameAsc(CatalogKind kind);

    Optional<CatalogItem> findByIdAndKind(Long id, CatalogKind kind);

    boolean existsByKindAndNameIgnoreCase(CatalogKind kind, String name);

    Optional<CatalogItem> findFirstByKindAndNameIgnoreCase(CatalogKind kind, String name);

    boolean existsByKindAndNameIgnoreCaseAndIdNot(CatalogKind kind, String name, Long id);

    long countByKind(CatalogKind kind);

    @Query("select coalesce(max(c.sortOrder), 0) from CatalogItem c where c.kind = :kind")
    int maxSortOrder(CatalogKind kind);
}
