package com.datavvy.vms.document.application;

import com.datavvy.vms.document.api.DocumentAlert;
import com.datavvy.vms.document.api.DocumentResponse;
import com.datavvy.vms.document.domain.VehicleDocument;
import com.datavvy.vms.platform.time.Expiry;
import java.time.LocalDate;

final class DocumentMapper {

    private DocumentMapper() {
    }

    static DocumentResponse toResponse(VehicleDocument d, String vehicleNumber, LocalDate today, int soonDays) {
        Expiry e = Expiry.of(d.getExpiryDate(), today, soonDays);
        return new DocumentResponse(d.getId(), d.getVehicleId(), vehicleNumber, d.getType(), d.getTitle(),
                d.getDocumentNumber(), d.getIssueDate(), d.getExpiryDate(), e.status(), e.daysLeft(), d.getNotes(),
                d.getFileName(), d.getContentType(), d.getSizeBytes(), d.getCreatedAt(), d.getUpdatedAt(),
                d.getUpdatedBy(), d.getVersion());
    }

    static DocumentAlert toAlert(VehicleDocument d, String vehicleNumber, LocalDate today, int soonDays) {
        Expiry e = Expiry.of(d.getExpiryDate(), today, soonDays);
        return new DocumentAlert(d.getId(), d.getVehicleId(), vehicleNumber, d.getType(), d.getTitle(),
                d.getExpiryDate(), e.status(), e.daysLeft());
    }
}
