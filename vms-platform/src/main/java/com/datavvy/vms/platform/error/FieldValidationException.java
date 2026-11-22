package com.datavvy.vms.platform.error;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Validation that needs service-level knowledge (e.g. "this vehicle type does
 * not exist"). Rendered exactly like bean-validation errors so the client has a
 * single shape to handle.
 */
public class FieldValidationException extends DomainException {

    private final List<FieldMessage> fieldErrors;

    public FieldValidationException(List<FieldMessage> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Some fields need attention.", Map.of());
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public FieldValidationException(String field, String message) {
        this(List.of(new FieldMessage(field, message)));
    }

    public List<FieldMessage> fieldErrors() {
        return fieldErrors;
    }

    public record FieldMessage(String field, String message) {
    }
}
