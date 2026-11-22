package com.datavvy.vms.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code vms.security.*}. The secret must come from the environment
 * ({@code VMS_JWT_SECRET}) outside local development.
 */
@Validated
@ConfigurationProperties(prefix = "vms.security")
public record AuthProperties(
        @NotBlank @Size(min = 32, message = "JWT secret must be at least 32 characters") String jwtSecret,
        String issuer,
        Duration tokenTtl,
        List<String> corsAllowedOrigins) {

    public AuthProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "datavvy-vms";
        }
        if (tokenTtl == null) {
            tokenTtl = Duration.ofHours(8);
        }
        if (corsAllowedOrigins == null) {
            corsAllowedOrigins = List.of();
        }
    }
}
