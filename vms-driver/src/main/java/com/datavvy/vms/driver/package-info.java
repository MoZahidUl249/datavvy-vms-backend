/**
 * Driver module: driver records (contact, licence and its expiry, status) and
 * driver ↔ vehicle assignment with full history. Owned by the Fleet Operations team.
 *
 * <p>Rule: a vehicle has at most one driver and a driver drives at most one
 * vehicle at a time — enforced by the service and by UNIQUE columns that hold
 * the ids only while an assignment is open.
 *
 * <p>Public surface: {@code com.datavvy.vms.driver.api}
 * ({@link com.datavvy.vms.driver.api.DriverQueryApi} for the dashboard).
 */
package com.datavvy.vms.driver;
