package com.datavvy.vms.driver.api;

import java.util.List;

/**
 * Driver list filters; null / empty = no filter. {@code q} matches name, phone and
 * licence number. {@code assigned}: true = has a vehicle, false = available.
 */
public record DriverFilter(String q, List<DriverStatus> statuses, Boolean assigned) {
}
