package com.datavvy.vms.catalog.domain;

import com.datavvy.vms.catalog.api.CatalogKind;
import com.datavvy.vms.platform.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_item")
public class CatalogItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CatalogKind kind;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CatalogItem() {
    }

    public CatalogItem(CatalogKind kind, String name, int sortOrder) {
        this.kind = kind;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public CatalogKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void rename(String newName) {
        this.name = newName;
    }
}
