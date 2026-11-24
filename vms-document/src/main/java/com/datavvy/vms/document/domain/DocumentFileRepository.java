package com.datavvy.vms.document.domain;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {

    @Modifying
    @Query("delete from DocumentFile f where f.documentId in :ids")
    void deleteByDocumentIds(@Param("ids") Collection<Long> ids);
}
