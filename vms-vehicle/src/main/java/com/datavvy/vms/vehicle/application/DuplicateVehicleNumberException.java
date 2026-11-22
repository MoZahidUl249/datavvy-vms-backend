package com.datavvy.vms.vehicle.application;

import com.datavvy.vms.platform.error.ConflictException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Business rule: a Vehicle Number can belong to only one vehicle.
 * Rendered as HTTP 409 with {@code code = DUPLICATE_VEHICLE_NUMBER}.
 * The message and details are safe to show to the client (no SQL, no stack).
 */
public class DuplicateVehicleNumberException extends ConflictException {

    public static final String CODE = "DUPLICATE_VEHICLE_NUMBER";

    public DuplicateVehicleNumberException(String vehicleNumber, Long existingId) {
        super(CODE, "A vehicle with number " + vehicleNumber + " already exists.", details(vehicleNumber, existingId));
    }

    private static Map<String, Object> details(String vehicleNumber, Long existingId) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("field", "vehicleNumber");
        d.put("vehicleNumber", vehicleNumber);
        if (existingId != null) {
            d.put("existingId", existingId);
        }
        return d;
    }
}
