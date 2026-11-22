package com.datavvy.vms.platform.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** The requested resource does not exist (or is not visible to the caller). */
public class NotFoundException extends DomainException {

    public NotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, resource.toUpperCase() + "_NOT_FOUND",
                resource + " " + id + " was not found.", Map.of("id", String.valueOf(id)));
    }
}
