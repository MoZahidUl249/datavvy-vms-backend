package com.datavvy.vms.platform.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** The request clashes with current state, e.g. a duplicate unique value. */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message, Map<String, Object> details) {
        super(HttpStatus.CONFLICT, code, message, details);
    }

    public ConflictException(String code, String message) {
        this(code, message, Map.of());
    }
}
