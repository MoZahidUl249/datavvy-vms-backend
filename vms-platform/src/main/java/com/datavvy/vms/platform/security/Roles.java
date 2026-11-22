package com.datavvy.vms.platform.security;

/**
 * Role names and ready-made {@code @PreAuthorize} expressions. Lives in the
 * platform so feature modules can guard endpoints without depending on the
 * auth module.
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String FLEET_MANAGER = "FLEET_MANAGER";
    public static final String VIEWER = "VIEWER";

    /** Read access: any signed-in user. */
    public static final String CAN_READ = "isAuthenticated()";
    /** Change fleet records. */
    public static final String CAN_MANAGE_FLEET = "hasAnyRole('ADMIN','FLEET_MANAGER')";
    /** Change workspace-wide settings such as catalogs. */
    public static final String CAN_ADMINISTER = "hasRole('ADMIN')";

    private Roles() {
    }
}
