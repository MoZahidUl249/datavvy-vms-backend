package com.datavvy.vms.auth.api;

/** Bearer token returned on sign-in. {@code expiresIn} is in seconds. */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, AuthUser user) {
}
