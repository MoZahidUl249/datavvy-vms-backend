package com.datavvy.vms.document.application;

import java.util.Collection;
import java.util.Optional;

/**
 * Where uploaded bytes live. Today: a database table (portable, backed up with
 * the data, nothing extra to run on a free host). For large fleets swap in an
 * object-storage implementation (S3 / Cloudflare R2) — the service doesn't change.
 */
public interface DocumentContentStore {

    void put(Long documentId, byte[] content);

    Optional<byte[]> get(Long documentId);

    void deleteAll(Collection<Long> documentIds);
}
