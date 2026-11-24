package com.datavvy.vms.document.api;

/** The papers a Bangladeshi fleet vehicle carries. {@code OTHER} needs a title. */
public enum DocumentType {
    REGISTRATION,
    TAX_TOKEN,
    INSURANCE,
    FITNESS,
    ROUTE_PERMIT,
    OTHER;

    /** Papers that are renewed periodically, so an expiry date is required. */
    public boolean expires() {
        return this == TAX_TOKEN || this == INSURANCE || this == FITNESS || this == ROUTE_PERMIT;
    }
}
