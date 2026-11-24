package com.datavvy.vms.document.application;

import com.datavvy.vms.document.api.DocumentFilter;
import com.datavvy.vms.document.api.DocumentRequest;
import com.datavvy.vms.document.api.DocumentResponse;
import com.datavvy.vms.document.api.DocumentType;
import com.datavvy.vms.document.config.DocumentProperties;
import com.datavvy.vms.document.domain.DocumentDetails;
import com.datavvy.vms.document.domain.DocumentSpecifications;
import com.datavvy.vms.document.domain.FileKind;
import com.datavvy.vms.document.domain.StoredFileInfo;
import com.datavvy.vms.document.domain.VehicleDocument;
import com.datavvy.vms.document.domain.VehicleDocumentRepository;
import com.datavvy.vms.platform.config.PlatformProperties;
import com.datavvy.vms.platform.error.ConflictException;
import com.datavvy.vms.platform.error.FieldValidationException;
import com.datavvy.vms.platform.error.FieldValidationException.FieldMessage;
import com.datavvy.vms.platform.error.NotFoundException;
import com.datavvy.vms.platform.web.PageResponse;
import com.datavvy.vms.vehicle.api.VehicleQueryApi;
import com.datavvy.vms.vehicle.api.VehicleSummary;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vehicle document use cases. Rules:
 * <ul>
 *   <li>The vehicle must exist (checked through {@code vehicle.api}).</li>
 *   <li>Periodic papers (tax token, insurance, fitness, route permit) need an expiry date;
 *       {@code OTHER} needs a title; expiry can't be before issue; issue can't be in the future.</li>
 *   <li>Files: PDF, JPG, PNG or WEBP recognised from the bytes, at most {@code vms.documents.max-file-size}.</li>
 *   <li>Updates send the version they loaded (optimistic locking).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DocumentService {

    private static final Set<String> SORTABLE = Set.of("expiryDate", "issueDate", "type", "createdAt", "updatedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "expiryDate");

    /** Expired first, then expiring, valid, and papers without an expiry date last. */
    private static final Comparator<DocumentResponse> ATTENTION_FIRST = Comparator
            .comparing((DocumentResponse d) -> d.expiryStatus().ordinal())
            .thenComparing(DocumentResponse::expiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(DocumentResponse::id);

    private final VehicleDocumentRepository documents;
    private final DocumentContentStore content;
    private final VehicleQueryApi vehicles;
    private final ZoneId zone;
    private final int soonDays;
    private final long maxBytes;

    public DocumentService(VehicleDocumentRepository documents, DocumentContentStore content,
            VehicleQueryApi vehicles, PlatformProperties platform, DocumentProperties props) {
        this.documents = documents;
        this.content = content;
        this.vehicles = vehicles;
        this.zone = platform.timeZone();
        this.soonDays = props.expiringSoonDays();
        this.maxBytes = props.maxFileSize().toBytes();
    }

    // ---------------------------------------------------------------- queries

    public List<DocumentResponse> listForVehicle(Long vehicleId) {
        VehicleSummary vehicle = requireVehicle(vehicleId);
        LocalDate today = today();
        return documents.findByVehicleId(vehicleId).stream()
                .map(d -> DocumentMapper.toResponse(d, vehicle.vehicleNumber(), today, soonDays))
                .sorted(ATTENTION_FIRST)
                .toList();
    }

    public PageResponse<DocumentResponse> search(DocumentFilter filter, Pageable pageable) {
        LocalDate today = today();
        Set<Long> idsForQ = filter.q() == null || filter.q().isBlank() ? Set.of() : vehicles.idsWithNumberLike(filter.q());
        Page<VehicleDocument> page = documents.findAll(
                DocumentSpecifications.matching(filter, idsForQ, today, soonDays), safe(pageable));
        Map<Long, VehicleSummary> byId = vehicles.findAll(page.getContent().stream().map(VehicleDocument::getVehicleId).toList());
        return PageResponse.of(page, d -> DocumentMapper.toResponse(d, numberOf(byId, d.getVehicleId()), today, soonDays));
    }

    public DocumentResponse get(Long id) {
        return toResponse(load(id));
    }

    public DownloadableFile download(Long id) {
        VehicleDocument d = load(id);
        byte[] bytes = content.get(id).orElseThrow(() -> new NotFoundException("Document", id));
        return new DownloadableFile(d.getFileName(), d.getContentType(), bytes);
    }

    // --------------------------------------------------------------- commands

    @Transactional
    public DocumentResponse upload(Long vehicleId, DocumentRequest request, String originalFileName, byte[] bytes) {
        VehicleSummary vehicle = requireVehicle(vehicleId);
        List<FieldMessage> errors = new ArrayList<>();
        DocumentDetails details = validate(request, errors);
        StoredFileInfo file = inspect(originalFileName, bytes, errors);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        VehicleDocument saved = documents.saveAndFlush(new VehicleDocument(vehicleId, details, file));
        content.put(saved.getId(), bytes);
        return DocumentMapper.toResponse(saved, vehicle.vehicleNumber(), today(), soonDays);
    }

    @Transactional
    public DocumentResponse update(Long id, DocumentRequest request) {
        VehicleDocument d = load(id);
        checkVersion(d, request.version());
        List<FieldMessage> errors = new ArrayList<>();
        DocumentDetails details = validate(request, errors);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        d.apply(details);
        return toResponse(documents.saveAndFlush(d));
    }

    /** Replaces the scan, e.g. after renewal. Metadata stays; update the expiry date with {@link #update}. */
    @Transactional
    public DocumentResponse replaceFile(Long id, String originalFileName, byte[] bytes) {
        VehicleDocument d = load(id);
        List<FieldMessage> errors = new ArrayList<>();
        StoredFileInfo file = inspect(originalFileName, bytes, errors);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        d.attach(file);
        content.put(id, bytes);
        return toResponse(documents.saveAndFlush(d));
    }

    @Transactional
    public void delete(Long id) {
        VehicleDocument d = load(id);
        content.deleteAll(List.of(id));
        documents.delete(d);
    }

    // ---------------------------------------------------------------- helpers

    DocumentDetails validate(DocumentRequest r, List<FieldMessage> errors) {
        if (r.type() == null) {
            errors.add(new FieldMessage("type", "Choose the document type, like INSURANCE."));
        }
        String title = tidy(r.title());
        if (r.type() == DocumentType.OTHER && title == null) {
            errors.add(new FieldMessage("title", "Name this document, like Pollution certificate."));
        }
        if (r.type() != null && r.type().expires() && r.expiryDate() == null) {
            errors.add(new FieldMessage("expiryDate", "Enter the expiry date printed on the paper, like 2027-06-30."));
        }
        if (r.issueDate() != null && r.issueDate().isAfter(today())) {
            errors.add(new FieldMessage("issueDate", "The issue date can't be in the future."));
        }
        if (r.issueDate() != null && r.expiryDate() != null && r.expiryDate().isBefore(r.issueDate())) {
            errors.add(new FieldMessage("expiryDate", "The expiry date must be on or after the issue date."));
        }
        checkLength(errors, "title", title, 120);
        checkLength(errors, "documentNumber", tidy(r.documentNumber()), 60);
        checkLength(errors, "notes", r.notes(), 500);
        return new DocumentDetails(r.type(), title, tidy(r.documentNumber()), r.issueDate(), r.expiryDate(),
                blankToNull(r.notes()));
    }

    StoredFileInfo inspect(String originalFileName, byte[] bytes, List<FieldMessage> errors) {
        if (bytes == null || bytes.length == 0) {
            errors.add(new FieldMessage("file", "Choose a file to upload: a PDF or a photo (JPG, PNG, WEBP)."));
            return null;
        }
        if (bytes.length > maxBytes) {
            errors.add(new FieldMessage("file", "Keep the file under " + (maxBytes / (1024 * 1024))
                    + " MB; scan at a lower resolution or upload a photo."));
            return null;
        }
        FileKind kind = FileKind.detect(bytes).orElse(null);
        if (kind == null) {
            errors.add(new FieldMessage("file", "Upload a PDF, JPG, PNG or WEBP file."));
            return null;
        }
        return new StoredFileInfo(safeFileName(originalFileName, kind), kind.contentType(), bytes.length);
    }

    /** Keeps a readable name without paths or odd characters, ending in the detected extension. */
    static String safeFileName(String original, FileKind kind) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        if (base.isBlank()) {
            base = "document";
        }
        if (base.length() > 150) {
            base = base.substring(0, 150);
        }
        return base + "." + kind.extension();
    }

    private void checkVersion(VehicleDocument d, Long version) {
        if (version == null || version != d.getVersion()) {
            throw new ConflictException("STALE_VERSION", "Someone else changed this document. Reload it and try again.",
                    Map.of("currentVersion", d.getVersion()));
        }
    }

    private DocumentResponse toResponse(VehicleDocument d) {
        String number = vehicles.find(d.getVehicleId()).map(VehicleSummary::vehicleNumber).orElse(null);
        return DocumentMapper.toResponse(d, number, today(), soonDays);
    }

    private VehicleSummary requireVehicle(Long vehicleId) {
        return vehicles.find(vehicleId).orElseThrow(() -> new NotFoundException("Vehicle", vehicleId));
    }

    private VehicleDocument load(Long id) {
        return documents.findById(id).orElseThrow(() -> new NotFoundException("Document", id));
    }

    private LocalDate today() {
        return LocalDate.now(zone);
    }

    private static String numberOf(Map<Long, VehicleSummary> byId, Long vehicleId) {
        VehicleSummary v = byId.get(vehicleId);
        return v == null ? null : v.vehicleNumber();
    }

    private static void checkLength(List<FieldMessage> errors, String field, String value, int max) {
        if (value != null && value.length() > max) {
            errors.add(new FieldMessage(field, "Keep this under " + max + " characters."));
        }
    }

    private static String tidy(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static Pageable safe(Pageable p) {
        List<Sort.Order> orders = p.getSort().stream().filter(o -> SORTABLE.contains(o.getProperty())).toList();
        Sort sort = (orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders)).and(Sort.by("id"));
        return PageRequest.of(p.getPageNumber(), p.getPageSize(), sort);
    }

}
