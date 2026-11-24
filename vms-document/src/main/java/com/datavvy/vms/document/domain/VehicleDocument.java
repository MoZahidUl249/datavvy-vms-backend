package com.datavvy.vms.document.domain;

import com.datavvy.vms.document.api.DocumentType;
import com.datavvy.vms.platform.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * One paper of one vehicle plus the metadata of its stored file. The bytes live
 * in {@link DocumentFile} so lists never load them. {@code vehicleId} refers to
 * the vehicle module by id only (ADR-0003).
 */
@Entity
@Table(name = "vehicle_document")
public class VehicleDocument extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType type;

    @Column(length = 120)
    private String title;

    @Column(name = "document_number", length = 60)
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 60)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    protected VehicleDocument() {
    }

    public VehicleDocument(Long vehicleId, DocumentDetails details, StoredFileInfo file) {
        this.vehicleId = vehicleId;
        apply(details);
        attach(file);
    }

    public void apply(DocumentDetails d) {
        this.type = d.type();
        this.title = d.title();
        this.documentNumber = d.documentNumber();
        this.issueDate = d.issueDate();
        this.expiryDate = d.expiryDate();
        this.notes = d.notes();
    }

    /** Records a new or replaced file (its bytes are saved separately). */
    public void attach(StoredFileInfo file) {
        this.fileName = file.fileName();
        this.contentType = file.contentType();
        this.sizeBytes = file.sizeBytes();
    }

    public Long getId() {
        return id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public DocumentType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
