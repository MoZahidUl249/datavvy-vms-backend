package com.datavvy.vms.document.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * The uploaded bytes, one row per document (same id). Kept apart from
 * {@link VehicleDocument} so listing documents never reads file content. This
 * is the database-backed implementation behind {@code DocumentContentStore};
 * swapping to object storage (S3 / R2) only replaces that store (ADR-0007).
 */
@Entity
@Table(name = "vehicle_document_file")
public class DocumentFile {

    @Id
    @Column(name = "document_id")
    private Long documentId;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", nullable = false)
    private byte[] content;

    protected DocumentFile() {
    }

    public DocumentFile(Long documentId, byte[] content) {
        this.documentId = documentId;
        this.content = content;
    }

    public void replace(byte[] newContent) {
        this.content = newContent;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public byte[] getContent() {
        return content;
    }
}
