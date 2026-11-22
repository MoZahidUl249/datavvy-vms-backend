package com.datavvy.vms.auth.api;

/** The signed-in user as exposed to clients and to other modules. */
public record AuthUser(Long id, String email, String displayName, String role) {
}
