package com.datavvy.vms.vehicle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datavvy.vms.catalog.api.CatalogItemRef;
import com.datavvy.vms.catalog.api.CatalogKind;
import com.datavvy.vms.catalog.api.CatalogLookup;
import com.datavvy.vms.platform.config.PlatformProperties;
import com.datavvy.vms.platform.error.ConflictException;
import com.datavvy.vms.platform.error.FieldValidationException;
import com.datavvy.vms.vehicle.api.VehicleRequest;
import com.datavvy.vms.vehicle.api.VehicleResponse;
import com.datavvy.vms.vehicle.api.VehicleStatus;
import com.datavvy.vms.vehicle.domain.Vehicle;
import com.datavvy.vms.vehicle.domain.VehicleDetails;
import com.datavvy.vms.vehicle.domain.VehicleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/** The duplicate Vehicle Number rule, tested at the business layer (no web, no database). */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    private static final String KEY = "DHAMETROGA114582";

    @Mock
    VehicleRepository vehicles;

    @Mock
    CatalogLookup catalog;

    VehicleService service;

    @BeforeEach
    void setUp() {
        service = new VehicleService(vehicles, catalog, new PlatformProperties(null));
    }

    @Test
    void createRejectsAnExistingVehicleNumber_evenWhenSpelledDifferently() {
        knownType();
        when(vehicles.findByVehicleNumberKey(KEY)).thenReturn(Optional.of(vehicle(7L, "DHA-METRO-GA 11-4582")));

        assertThatThrownBy(() -> service.create(request("dha metro ga 11 4582", null)))
                .isInstanceOf(DuplicateVehicleNumberException.class)
                .hasMessageContaining("DHA-METRO-GA 11-4582")
                .satisfies(e -> {
                    ConflictException c = (ConflictException) e;
                    assertThat(c.code()).isEqualTo("DUPLICATE_VEHICLE_NUMBER");
                    assertThat(c.details()).containsEntry("existingId", 7L);
                });
        verify(vehicles, never()).saveAndFlush(any());
    }

    @Test
    void createSavesTheCanonicalNumberWhenItIsNew() {
        knownType();
        when(vehicles.findByVehicleNumberKey(KEY)).thenReturn(Optional.empty());
        when(vehicles.saveAndFlush(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        VehicleResponse saved = service.create(request("dha metro ga 11 4582", null));

        assertThat(saved.vehicleNumber()).isEqualTo("DHA-METRO-GA 11-4582");
        assertThat(saved.type()).isEqualTo("Microbus");
        assertThat(saved.status()).isEqualTo(VehicleStatus.ACTIVE);
    }

    @Test
    void updateRejectsTheNumberOfAnotherVehicle() {
        knownType();
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle(1L, "SYL-GA 11-0458")));
        when(vehicles.findByVehicleNumberKey(KEY)).thenReturn(Optional.of(vehicle(2L, "DHA-METRO-GA 11-4582")));

        assertThatThrownBy(() -> service.update(1L, request("DHA-METRO-GA 11-4582", 0L)))
                .isInstanceOf(DuplicateVehicleNumberException.class);
        verify(vehicles, never()).saveAndFlush(any());
    }

    @Test
    void updateMayKeepItsOwnNumber() {
        knownType();
        Vehicle self = vehicle(1L, "DHA-METRO-GA 11-4582");
        when(vehicles.findById(1L)).thenReturn(Optional.of(self));
        when(vehicles.findByVehicleNumberKey(KEY)).thenReturn(Optional.of(self));
        when(vehicles.saveAndFlush(self)).thenReturn(self);

        assertThat(service.update(1L, request("DHA-METRO-GA 11-4582", 0L)).id()).isEqualTo(1L);
    }

    @Test
    void concurrentDuplicateCaughtByTheUniqueConstraintIsStillAClientSafeConflict() {
        knownType();
        when(vehicles.findByVehicleNumberKey(KEY)).thenReturn(Optional.empty());
        when(vehicles.saveAndFlush(any(Vehicle.class))).thenThrow(new DataIntegrityViolationException("uk_vehicle_number_key"));

        assertThatThrownBy(() -> service.create(request("DHA-METRO-GA 11-4582", null)))
                .isInstanceOf(DuplicateVehicleNumberException.class)
                .hasMessageNotContaining("uk_vehicle_number_key");
    }

    @Test
    void invalidNumberAndUnknownTypeAreFieldErrors() {
        when(catalog.findByName(CatalogKind.VEHICLE_TYPE, "Spaceship")).thenReturn(Optional.empty());

        VehicleRequest bad = new VehicleRequest("DHA 1", "Datavvy", "Spaceship", "Toyota", "Hiace",
                VehicleStatus.ACTIVE, null);
        assertThatThrownBy(() -> service.create(bad))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(e -> assertThat(((FieldValidationException) e).fieldErrors())
                        .extracting(FieldValidationException.FieldMessage::field)
                        .containsExactly("vehicleNumber", "type"));
    }

    @Test
    void updateWithAStaleVersionIsRejected() {
        when(vehicles.findById(1L)).thenReturn(Optional.of(vehicle(1L, "DHA-METRO-GA 11-4582")));

        assertThatThrownBy(() -> service.update(1L, request("DHA-METRO-GA 11-4582", 5L)))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((ConflictException) e).code())
                .isEqualTo("STALE_VERSION");
    }

    // ------------------------------------------------------------------ helpers

    private void knownType() {
        when(catalog.findByName(CatalogKind.VEHICLE_TYPE, "microbus"))
                .thenReturn(Optional.of(new CatalogItemRef(2L, "Microbus")));
    }

    private static VehicleRequest request(String number, Long version) {
        return new VehicleRequest(number, "Datavvy Logistics", "microbus", "Toyota", "Hiace",
                VehicleStatus.ACTIVE, version);
    }

    private static Vehicle vehicle(Long id, String number) {
        Vehicle v = new Vehicle(new VehicleDetails(number, "Datavvy Logistics", "Microbus", "Toyota", "Hiace",
                VehicleStatus.ACTIVE));
        ReflectionTestUtils.setField(v, "id", id);
        return v;
    }
}
