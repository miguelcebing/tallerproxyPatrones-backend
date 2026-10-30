package com.miguelcebing.tallerproxy.domain;

import java.time.Instant;

public record LogQuery(
        String serviceId,
        CallStatus status,
        Instant from,
        Instant to,
        int page,
        int size) {
}
