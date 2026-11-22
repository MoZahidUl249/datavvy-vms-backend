package com.datavvy.vms.auth.application;

import com.datavvy.vms.platform.error.DomainException;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Same message for unknown email and wrong password, so accounts can't be probed. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "That email and password don't match. Check both and try again.", Map.of());
    }
}
