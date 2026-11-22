-- Module: catalog  | Owner: @datavvy/fleet-settings-team
-- Reference lists managed on the Settings screen (vehicle types). Default values
-- are product reference data (not demo data), so they ship with the schema.

CREATE TABLE catalog_item (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    kind             VARCHAR(40)  NOT NULL,
    name             VARCHAR(60)  NOT NULL,
    sort_order       INT          NOT NULL DEFAULT 0,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    created_by       VARCHAR(120) NULL,
    updated_by       VARCHAR(120) NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_catalog_item_kind_name UNIQUE (kind, name)
);

INSERT INTO catalog_item (kind, name, sort_order, created_at, updated_at, created_by, updated_by, version) VALUES
    ('VEHICLE_TYPE', 'Sedan',      1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system', 0),
    ('VEHICLE_TYPE', 'Microbus',   2, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system', 0),
    ('VEHICLE_TYPE', 'Pickup',     3, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system', 0),
    ('VEHICLE_TYPE', 'Truck',      4, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system', 0),
    ('VEHICLE_TYPE', 'Bus',        5, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system', 0),
    ('VEHICLE_TYPE', 'Motorcycle', 6, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system', 0);
