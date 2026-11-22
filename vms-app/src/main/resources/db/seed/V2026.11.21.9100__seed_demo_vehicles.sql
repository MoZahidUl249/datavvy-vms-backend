-- LOCAL / DEMO ONLY: the eight vehicles from the V8 design mock data, mapped to the course fields.
INSERT INTO vehicle (vehicle_number, vehicle_number_key, company, type, brand, model, status,
                     created_at, updated_at, created_by, updated_by, version) VALUES
    ('DHA-METRO-GA 11-4582', 'DHAMETROGA114582', 'Datavvy Logistics', 'Microbus', 'Toyota', 'Hiace', 'ACTIVE', '2022-02-14 09:30:00', '2026-11-19 10:15:00', 'seed', 'seed', 0),
    ('CTG-METRO-TA 12-0931', 'CTGMETROTA120931', 'Datavvy Logistics', 'Truck', 'Isuzu', 'NPR', 'ACTIVE', '2021-06-03 09:30:00', '2026-11-17 10:15:00', 'seed', 'seed', 0),
    ('DHA-METRO-KHA 14-2210', 'DHAMETROKHA142210', 'Datavvy Transport', 'Sedan', 'Toyota', 'Axio', 'ACTIVE', '2026-11-12 09:30:00', '2026-11-12 10:15:00', 'seed', 'seed', 0),
    ('DHA-METRO-GHA 19-7710', 'DHAMETROGHA197710', 'Datavvy Transport', 'Pickup', 'Mitsubishi', 'L200', 'ACTIVE', '2026-10-28 09:30:00', '2026-11-08 10:15:00', 'seed', 'seed', 0),
    ('DHA-METRO-CHA 15-3307', 'DHAMETROCHA153307', 'Datavvy Logistics', 'Truck', 'Tata', 'Ultra 1014', 'INACTIVE', '2020-09-10 09:30:00', '2026-10-30 10:15:00', 'seed', 'seed', 0),
    ('SYL-GA 11-0458', 'SYLGA110458', 'Datavvy Sylhet', 'Microbus', 'Toyota', 'Noah', 'ACTIVE', '2026-10-18 09:30:00', '2026-10-21 10:15:00', 'seed', 'seed', 0),
    ('DHA-METRO-HA 21-6612', 'DHAMETROHA216612', 'Datavvy Transport', 'Motorcycle', 'Honda', 'CB Hornet 160R', 'ACTIVE', '2025-03-22 09:30:00', '2026-10-14 10:15:00', 'seed', 'seed', 0),
    ('DHA-METRO-BA 11-2087', 'DHAMETROBA112087', 'Datavvy Logistics', 'Bus', 'Hino', 'AK1J', 'MAINTENANCE', '2019-05-30 09:30:00', '2026-09-28 10:15:00', 'seed', 'seed', 0);
