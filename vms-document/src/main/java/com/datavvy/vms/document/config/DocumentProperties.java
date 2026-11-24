package com.datavvy.vms.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * {@code vms.documents.*}
 *
 * @param expiringSoonDays a document is "expiring soon" this many days before its expiry date (default 30)
 * @param maxFileSize      largest accepted upload (default 5 MB; keep the servlet multipart limit above it)
 */
@ConfigurationProperties(prefix = "vms.documents")
public record DocumentProperties(Integer expiringSoonDays, DataSize maxFileSize) {

    public DocumentProperties {
        if (expiringSoonDays == null) {
            expiringSoonDays = 30;
        }
        if (maxFileSize == null) {
            maxFileSize = DataSize.ofMegabytes(5);
        }
    }
}
