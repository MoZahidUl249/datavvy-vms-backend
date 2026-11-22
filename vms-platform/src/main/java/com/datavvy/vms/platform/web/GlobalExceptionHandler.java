package com.datavvy.vms.platform.web;

import com.datavvy.vms.platform.error.DomainException;
import com.datavvy.vms.platform.error.FieldValidationException;
import com.datavvy.vms.platform.error.FieldValidationException.FieldMessage;
import jakarta.validation.ConstraintViolationException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every failure into an RFC 9457 problem document:
 * <pre>
 * { "type": "about:blank", "title": "Conflict", "status": 409,
 *   "detail": "...", "code": "DUPLICATE_VEHICLE_NUMBER",
 *   "fieldErrors": [ { "field": "...", "message": "..." } ],
 *   "details": { ... } }
 * </pre>
 * The {@code code} property is the contract clients branch on.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FieldValidationException.class)
    ProblemDetail handleFieldValidation(FieldValidationException ex) {
        return validationProblem(ex.fieldErrors());
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setProperty("code", ex.code());
        if (!ex.details().isEmpty()) {
            pd.setProperty("details", ex.details());
        }
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldMessage> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldMessage(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return validationProblem(errors);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Someone else changed this record. Reload it and try again.");
        pd.setProperty("code", "STALE_VERSION");
        return pd;
    }

    /** {@code @PreAuthorize} failures must stay 403, not fall into the catch-all 500. */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Your role can't do this. Ask an admin if you need access.");
        pd.setProperty("code", "FORBIDDEN");
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The change clashes with existing data.");
        pd.setProperty("code", "DATA_CONFLICT");
        return pd;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldMessage> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldMessage(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    /** Unparseable JSON or a value of the wrong type (e.g. status "BROKEN"). No parser internals leak. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String field = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            Class<?> target = ife.getTargetType();
            String message = target != null && target.isEnum()
                    ? "Use one of: " + String.join(", ", Arrays.stream(target.getEnumConstants()).map(String::valueOf).toList()) + "."
                    : "This value has the wrong type.";
            return ResponseEntity.badRequest().body(validationProblem(List.of(new FieldMessage(field, message))));
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request body isn't valid JSON. Check commas, quotes and brackets.");
        pd.setProperty("code", "MALFORMED_REQUEST");
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. Try again in a moment.");
        pd.setProperty("code", "INTERNAL_ERROR");
        return pd;
    }

    private static ProblemDetail validationProblem(List<FieldMessage> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Some fields need attention.");
        pd.setProperty("code", "VALIDATION_FAILED");
        pd.setProperty("fieldErrors", errors);
        return pd;
    }

    private static String lastNode(String path) {
        int i = path.lastIndexOf('.');
        return i < 0 ? path : path.substring(i + 1);
    }
}
