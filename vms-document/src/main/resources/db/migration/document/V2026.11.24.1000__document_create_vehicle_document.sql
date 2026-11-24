-- Module: document  | Owner: @datavvy/fleet-compliance-team
-- A vehicle's papers (registration, tax token, insurance, fitness, route permit, other)
-- with expiry tracking, plus the uploaded file.
--
-- vehicle_id   id of a vehicle in the vehicle module; no FOREIGN KEY across modules (ADR-0003).
--              Rows are removed when the vehicle is deleted (VehicleDeletedEvent).
-- expiry_date  NULL for papers that don't expire (e.g. registration). Indexed: the
--              expired / expiring-soon filters and the dashboard are date-range queries.
-- content_type detected from the file's first bytes, not taken from the browser.

CREATE TABLE vehicle_document (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    vehicle_id      BIGINT       NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    title           VARCHAR(120) NULL,
    document_number VARCHAR(60)  NULL,
    issue_date      DATE         NULL,
    expiry_date     DATE         NULL,
    notes           VARCHAR(500) NULL,
    file_name       VARCHAR(200) NOT NULL,
    content_type    VARCHAR(60)  NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(120) NULL,
    updated_by      VARCHAR(120) NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ck_vehicle_document_type
        CHECK (type IN ('REGISTRATION', 'TAX_TOKEN', 'INSURANCE', 'FITNESS', 'ROUTE_PERMIT', 'OTHER')),
    CONSTRAINT ck_vehicle_document_dates
        CHECK (issue_date IS NULL OR expiry_date IS NULL OR expiry_date >= issue_date)
);

CREATE INDEX idx_vehicle_document_vehicle ON vehicle_document (vehicle_id);
CREATE INDEX idx_vehicle_document_expiry  ON vehicle_document (expiry_date);
CREATE INDEX idx_vehicle_document_type    ON vehicle_document (type);

-- The bytes, one row per document. Separate table so lists never read file content.
-- 1:1 extension of vehicle_document (same id), so it carries no audit columns of its own.
CREATE TABLE vehicle_document_file (
    document_id BIGINT     NOT NULL,
    content     MEDIUMBLOB NOT NULL,
    PRIMARY KEY (document_id),
    CONSTRAINT fk_vehicle_document_file_document FOREIGN KEY (document_id) REFERENCES vehicle_document (id)
);
