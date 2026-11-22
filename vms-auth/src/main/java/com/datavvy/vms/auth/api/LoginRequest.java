package com.datavvy.vms.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Enter your email, like name@company.com.")
        @Email(message = "Enter a valid email, like name@company.com.")
        String email,
        @NotBlank(message = "Enter your password.")
        String password) {
}
