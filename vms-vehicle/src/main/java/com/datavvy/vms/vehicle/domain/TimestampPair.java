package com.datavvy.vms.vehicle.domain;

import java.time.Instant;

public record TimestampPair(Instant createdAt, Instant updatedAt) {
}
