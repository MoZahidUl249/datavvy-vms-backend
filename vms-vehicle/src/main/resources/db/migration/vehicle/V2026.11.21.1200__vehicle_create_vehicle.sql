-- Module: vehicle  | Owner: @datavvy/fleet-core-team
-- Vehicle with the course fields: Vehicle Number, Company, Type, Brand, Model, Status.
--
-- id                  generated surrogate primary key (BIGINT AUTO_INCREMENT)
-- vehicle_number      display form, e.g. 'DHA-METRO-GA 11-4582'
-- vehicle_number_key  normalised form, e.g. 'DHAMETROGA114582' (uppercase, no spaces/hyphens).
--                     UNIQUE: the database-level guarantee behind the service's
--                     duplicate rule, so 'dha metro ga 11 4582' can't sneak in twice.
-- type                name of a catalog vehicle type; no FOREIGN KEY because the catalog
--                     is another module (ADR-0003) — validated in VehicleService.
-- status              enum name, constrained by CHECK (MySQL 8.0.16+ and H2 enforce it).

CREATE TABLE vehicle (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    vehicle_number     VARCHAR(32)  NOT NULL,
    vehicle_number_key VARCHAR(32)  NOT NULL,
    company            VARCHAR(100) NOT NULL,
    type               VARCHAR(60)  NOT NULL,
    brand              VARCHAR(60)  NOT NULL,
    model              VARCHAR(60)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    created_by         VARCHAR(120) NULL,
    updated_by         VARCHAR(120) NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_vehicle_number_key UNIQUE (vehicle_number_key),
    CONSTRAINT ck_vehicle_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
);

CREATE INDEX idx_vehicle_status     ON vehicle (status);
CREATE INDEX idx_vehicle_type       ON vehicle (type);
CREATE INDEX idx_vehicle_company    ON vehicle (company);
CREATE INDEX idx_vehicle_brand      ON vehicle (brand);
CREATE INDEX idx_vehicle_updated_at ON vehicle (updated_at);
CREATE INDEX idx_vehicle_created_at ON vehicle (created_at);
