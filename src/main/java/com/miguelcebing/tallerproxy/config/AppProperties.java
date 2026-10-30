package com.miguelcebing.tallerproxy.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Simulation simulation,
        Logging logging,
        Metrics metrics,
        Pagination pagination) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Simulation(
            double paymentFailureRate,
            long minLatencyMs,
            long maxLatencyMs,
            int loadRequests,
            int loadThreads) {
    }

    public record Logging(int capacity, int stackTraceFrames) {
    }

    public record Metrics(double degradedErrorRateThreshold) {
    }

    public record Pagination(int defaultSize, int maxSize) {
    }
}
