package com.datavvy.vms.document.web;

import com.datavvy.vms.document.api.DocumentFilter;
import com.datavvy.vms.document.api.DocumentRequest;
import com.datavvy.vms.document.api.DocumentResponse;
import com.datavvy.vms.document.api.DocumentStats;
import com.datavvy.vms.document.api.DocumentType;
import com.datavvy.vms.document.application.DocumentService;
import com.datavvy.vms.document.application.DownloadableFile;
import com.datavvy.vms.document.api.DocumentQueryApi;
import com.datavvy.vms.platform.error.FieldValidationException;
import com.datavvy.vms.platform.error.FieldValidationException.FieldMessage;
import com.datavvy.vms.platform.security.Roles;
import com.datavvy.vms.platform.time.ExpiryStatus;
import com.datavvy.vms.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Vehicle documents.
 *
 * <pre>
 * GET    /api/v1/vehicles/{vehicleId}/documents   200 list (expired first) | 404
 * POST   /api/v1/vehicles/{vehicleId}/documents   multipart file + fields → 201 | 400 | 404 | 413
 * GET    /api/v1/documents                        200 page (fleet-wide; expiry, type, q, vehicleId)
 * GET    /api/v1/documents/summary                200 counts by expiry state
 * GET    /api/v1/documents/{id}                   200 | 404
 * PUT    /api/v1/documents/{id}                   JSON details → 200 | 400 | 404 | 409 STALE_VERSION
 * GET    /api/v1/documents/{id}/file              200 bytes | 404
 * POST   /api/v1/documents/{id}/file              multipart file → 200 | 400 | 404
 * DELETE /api/v1/documents/{id}                   204 | 404
 * </pre>
 */
@RestController
public class DocumentController {

    private final DocumentService service;
    private final DocumentQueryApi queries;

    public DocumentController(DocumentService service, DocumentQueryApi queries) {
        this.service = service;
        this.queries = queries;
    }

    @GetMapping("/api/v1/vehicles/{vehicleId}/documents")
    @PreAuthorize(Roles.CAN_READ)
    public List<DocumentResponse> listForVehicle(@PathVariable Long vehicleId) {
        return service.listForVehicle(vehicleId);
    }

    /** Multipart form: {@code file} plus {@code type, title, documentNumber, issueDate, expiryDate, notes}. */
    @PostMapping(path = "/api/v1/vehicles/{vehicleId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public ResponseEntity<DocumentResponse> upload(@PathVariable Long vehicleId,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) String issueDate,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) String notes) {
        List<FieldMessage> errors = new ArrayList<>();
        DocumentRequest request = new DocumentRequest(parseType(type, errors), title, documentNumber,
                parseDate("issueDate", issueDate, errors), parseDate("expiryDate", expiryDate, errors), notes, null);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        DocumentResponse created = service.upload(vehicleId, request, nameOf(file), bytesOf(file));
        return ResponseEntity.created(URI.create("/api/v1/documents/" + created.id())).body(created);
    }

    /** {@code GET /api/v1/documents?expiry=EXPIRED&expiry=EXPIRING_SOON&type=INSURANCE&q=ga%2011&page=0&size=20&sort=expiryDate,asc} */
    @GetMapping("/api/v1/documents")
    @PreAuthorize(Roles.CAN_READ)
    public PageResponse<DocumentResponse> search(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(name = "type", required = false) List<DocumentType> types,
            @RequestParam(name = "expiry", required = false) List<ExpiryStatus> expiry,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "expiryDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(new DocumentFilter(vehicleId, types, expiry, q), pageable);
    }

    @GetMapping("/api/v1/documents/summary")
    @PreAuthorize(Roles.CAN_READ)
    public DocumentStats summary() {
        return queries.stats();
    }

    @GetMapping("/api/v1/documents/{id}")
    @PreAuthorize(Roles.CAN_READ)
    public DocumentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/api/v1/documents/{id}")
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public DocumentResponse update(@PathVariable Long id, @Valid @RequestBody DocumentRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/api/v1/documents/{id}/file")
    @PreAuthorize(Roles.CAN_READ)
    public ResponseEntity<byte[]> download(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean inline) {
        DownloadableFile f = service.download(id);
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(f.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(f.content());
    }

    @PostMapping(path = "/api/v1/documents/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public DocumentResponse replaceFile(@PathVariable Long id, @RequestParam(required = false) MultipartFile file) {
        return service.replaceFile(id, nameOf(file), bytesOf(file));
    }

    @DeleteMapping("/api/v1/documents/{id}")
    @PreAuthorize(Roles.CAN_MANAGE_FLEET)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -- multipart form fields arrive as text; turn bad values into field errors, not 500s

    private static DocumentType parseType(String raw, List<FieldMessage> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return DocumentType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errors.add(new FieldMessage("type",
                    "Use one of: REGISTRATION, TAX_TOKEN, INSURANCE, FITNESS, ROUTE_PERMIT, OTHER."));
            return null;
        }
    }

    private static LocalDate parseDate(String field, String raw, List<FieldMessage> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            errors.add(new FieldMessage(field, "Use the date format YYYY-MM-DD, like 2027-06-30."));
            return null;
        }
    }

    private static String nameOf(MultipartFile file) {
        return file == null ? null : file.getOriginalFilename();
    }

    private static byte[] bytesOf(MultipartFile file) {
        if (file == null) {
            return null;
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
