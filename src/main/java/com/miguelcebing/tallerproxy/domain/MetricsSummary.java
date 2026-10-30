package com.miguelcebing.tallerproxy.domain;

import java.util.List;

public record MetricsSummary(
        long totalCalls,
        long errorCalls,
        double errorRate,
        double avgResponseTimeMs,
        List<ServiceMetrics> services) {
}
