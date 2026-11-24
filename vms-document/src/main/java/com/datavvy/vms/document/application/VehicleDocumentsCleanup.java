package com.datavvy.vms.document.application;

import com.datavvy.vms.document.domain.VehicleDocumentRepository;
import com.datavvy.vms.vehicle.api.VehicleDeletedEvent;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** A deleted vehicle takes its papers with it (same transaction as the vehicle delete). */
@Component
class VehicleDocumentsCleanup {

    private final VehicleDocumentRepository documents;
    private final DocumentContentStore content;

    VehicleDocumentsCleanup(VehicleDocumentRepository documents, DocumentContentStore content) {
        this.documents = documents;
        this.content = content;
    }

    @EventListener
    @Transactional
    public void on(VehicleDeletedEvent event) {
        List<Long> ids = documents.idsForVehicle(event.vehicleId());
        content.deleteAll(ids);
        documents.deleteAllById(ids);
    }
}
