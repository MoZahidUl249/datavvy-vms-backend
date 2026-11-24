-- Module: maintenance  | Owner: @datavvy/fleet-operations-team
-- Service and repair records; a vehicle's maintenance history is its rows by service_date.
--
-- vehicle_id  id of a vehicle in the vehicle module; no FOREIGN KEY across modules (ADR-0003).
--             Rows are removed when the vehicle is deleted (VehicleDeletedEvent).
-- cost        BDT, two decimals; DECIMAL so sums are exact.

CREATE TABLE maintenance_record (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    vehicle_id   BIGINT        NOT NULL,
    service_date DATE          NOT NULL,
    type         VARCHAR(30)   NOT NULL,
    cost         DECIMAL(12,2) NOT NULL,
    vendor       VARCHAR(120)  NOT NULL,
    odometer_km  INT           NULL,
    notes        VARCHAR(1000) NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    created_by   VARCHAR(120)  NULL,
    updated_by   VARCHAR(120)  NULL,
    version      BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ck_maintenance_cost CHECK (cost >= 0),
    CONSTRAINT ck_maintenance_type CHECK (type IN ('ROUTINE_SERVICE', 'OIL_CHANGE', 'TYRES', 'BATTERY', 'BRAKES',
        'ENGINE', 'ELECTRICAL', 'BODY_WORK', 'INSPECTION', 'REPAIR', 'OTHER'))
);

CREATE INDEX idx_maintenance_vehicle_date ON maintenance_record (vehicle_id, service_date);
CREATE INDEX idx_maintenance_service_date ON maintenance_record (service_date);
CREATE INDEX idx_maintenance_type         ON maintenance_record (type);
