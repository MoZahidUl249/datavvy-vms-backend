package com.datavvy.vms.platform.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Base class for every expected business failure. The global handler turns it
 * into an RFC 9457 problem response carrying {@link #code()} so clients can
 * branch on a stable machine-readable value instead of on message text.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    protected DomainException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public Map<String, Object> details() {
        return details;
    }
}
