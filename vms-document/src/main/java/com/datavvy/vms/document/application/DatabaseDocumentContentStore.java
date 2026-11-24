package com.datavvy.vms.document.application;

import com.datavvy.vms.document.domain.DocumentFile;
import com.datavvy.vms.document.domain.DocumentFileRepository;
import java.util.Collection;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** {@link DocumentContentStore} in the {@code vehicle_document_file} table. */
@Component
class DatabaseDocumentContentStore implements DocumentContentStore {

    private final DocumentFileRepository files;

    DatabaseDocumentContentStore(DocumentFileRepository files) {
        this.files = files;
    }

    @Override
    public void put(Long documentId, byte[] content) {
        DocumentFile file = files.findById(documentId).orElse(null);
        if (file == null) {
            files.save(new DocumentFile(documentId, content));
        } else {
            file.replace(content);
        }
    }

    @Override
    public Optional<byte[]> get(Long documentId) {
        return files.findById(documentId).map(DocumentFile::getContent);
    }

    @Override
    public void deleteAll(Collection<Long> documentIds) {
        if (!documentIds.isEmpty()) {
            files.deleteByDocumentIds(documentIds);
        }
    }
}
