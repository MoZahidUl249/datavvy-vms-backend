package com.datavvy.vms.platform.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** The request is well-formed but breaks a business rule (HTTP 422). */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String code, String message, Map<String, Object> details) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message, details);
    }

    public BusinessRuleException(String code, String message) {
        this(code, message, Map.of());
    }
}
