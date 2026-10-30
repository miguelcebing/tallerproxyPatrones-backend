package com.miguelcebing.tallerproxy.domain;

public record ServiceMetrics(
        String serviceId,
        long totalCalls,
        long successCalls,
        long errorCalls,
        double successRate,
        double errorRate,
        double avgResponseTimeMs,
        boolean degraded) {
}
