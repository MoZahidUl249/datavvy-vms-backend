-- Module: driver  | Owner: @datavvy/fleet-operations-team
-- Drivers and driver <-> vehicle assignments (with history).

-- license_number_key  normalised licence number (uppercase, no spaces/hyphens/slashes); UNIQUE.
-- phone               canonical Bangladeshi mobile, 01XXXXXXXXX.
CREATE TABLE driver (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    full_name          VARCHAR(100) NOT NULL,
    phone              VARCHAR(20)  NOT NULL,
    license_number     VARCHAR(30)  NOT NULL,
    license_number_key VARCHAR(30)  NOT NULL,
    license_expiry     DATE         NOT NULL,
    address            VARCHAR(200) NULL,
    joined_on          DATE         NULL,
    status             VARCHAR(20)  NOT NULL,
    notes              VARCHAR(500) NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    created_by         VARCHAR(120) NULL,
    updated_by         VARCHAR(120) NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_driver_license_number_key UNIQUE (license_number_key),
    CONSTRAINT ck_driver_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'INACTIVE'))
);

CREATE INDEX idx_driver_full_name      ON driver (full_name);
CREATE INDEX idx_driver_status         ON driver (status);
CREATE INDEX idx_driver_license_expiry ON driver (license_expiry);

-- One row per assignment; released_at NULL = current.
-- vehicle_id          id in the vehicle module; no FOREIGN KEY across modules (ADR-0003).
-- driver_name,
-- vehicle_number      snapshots, so history reads well after either side is deleted.
-- open_vehicle_id,
-- open_driver_id      copies of the ids while the assignment is open, NULL once released.
--                     UNIQUE: the database refuses a second open assignment for the same
--                     vehicle or driver. NULLs never clash (MySQL and H2), so history rows
--                     don't block anything.
CREATE TABLE driver_assignment (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    driver_id       BIGINT       NOT NULL,
    vehicle_id      BIGINT       NOT NULL,
    driver_name     VARCHAR(100) NOT NULL,
    vehicle_number  VARCHAR(32)  NOT NULL,
    released_at     DATETIME(6)  NULL,
    open_vehicle_id BIGINT       NULL,
    open_driver_id  BIGINT       NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(120) NULL,
    updated_by      VARCHAR(120) NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_driver_assignment_open_vehicle UNIQUE (open_vehicle_id),
    CONSTRAINT uk_driver_assignment_open_driver  UNIQUE (open_driver_id)
);

CREATE INDEX idx_driver_assignment_vehicle ON driver_assignment (vehicle_id);
CREATE INDEX idx_driver_assignment_driver  ON driver_assignment (driver_id);
