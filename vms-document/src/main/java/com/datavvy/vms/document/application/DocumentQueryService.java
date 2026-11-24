package com.datavvy.vms.document.application;

import com.datavvy.vms.document.api.DocumentAlert;
import com.datavvy.vms.document.api.DocumentQueryApi;
import com.datavvy.vms.document.api.DocumentStats;
import com.datavvy.vms.document.config.DocumentProperties;
import com.datavvy.vms.document.domain.VehicleDocument;
import com.datavvy.vms.document.domain.VehicleDocumentRepository;
import com.datavvy.vms.platform.config.PlatformProperties;
import com.datavvy.vms.platform.time.Expiry;
import com.datavvy.vms.vehicle.api.VehicleQueryApi;
import com.datavvy.vms.vehicle.api.VehicleSummary;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DocumentQueryService implements DocumentQueryApi {

    private final VehicleDocumentRepository documents;
    private final VehicleQueryApi vehicles;
    private final ZoneId zone;
    private final int soonDays;

    DocumentQueryService(VehicleDocumentRepository documents, VehicleQueryApi vehicles, PlatformProperties platform,
            DocumentProperties props) {
        this.documents = documents;
        this.vehicles = vehicles;
        this.zone = platform.timeZone();
        this.soonDays = props.expiringSoonDays();
    }

    @Override
    public DocumentStats stats() {
        LocalDate today = LocalDate.now(zone);
        LocalDate soon = Expiry.soonLimit(today, soonDays);
        long expired = documents.countByExpiryDateBefore(today);
        long expiring = documents.countByExpiryDateBetween(today, soon);
        long valid = documents.countByExpiryDateAfter(soon);
        long none = documents.countByExpiryDateIsNull();
        return new DocumentStats(expired + expiring + valid + none, valid, expiring, expired, none, soonDays);
    }

    @Override
    public List<DocumentAlert> needingAttention(int limit) {
        LocalDate today = LocalDate.now(zone);
        List<VehicleDocument> due = documents.findExpiringOnOrBefore(Expiry.soonLimit(today, soonDays),
                PageRequest.of(0, limit));
        Map<Long, VehicleSummary> byId = vehicles.findAll(due.stream().map(VehicleDocument::getVehicleId).toList());
        return due.stream().map(d -> DocumentMapper.toAlert(d,
                byId.containsKey(d.getVehicleId()) ? byId.get(d.getVehicleId()).vehicleNumber() : null,
                today, soonDays)).toList();
    }
}
