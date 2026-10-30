package com.miguelcebing.tallerproxy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.miguelcebing.tallerproxy.config.AppProperties;
import com.miguelcebing.tallerproxy.domain.LogEntry;
import com.miguelcebing.tallerproxy.domain.MetricsSummary;
import com.miguelcebing.tallerproxy.domain.ServiceMetrics;
import com.miguelcebing.tallerproxy.logging.InMemoryLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetricsServiceTest {

    private static final Instant BASE = Instant.parse("2026-10-30T10:00:00Z");

    @Test
    void shouldReturnZeroesAndAllServices_whenThereAreNoLogs() {
        MetricsService metricsService = metricsService(new InMemoryLogRepository(100));

        MetricsSummary summary = metricsService.summarize();

        assertThat(summary.totalCalls()).isZero();
        assertThat(summary.errorRate()).isZero();
        assertThat(summary.avgResponseTimeMs()).isZero();
        assertThat(summary.services()).hasSize(3);
        assertThat(summary.services()).allSatisfy(service -> {
            assertThat(service.totalCalls()).isZero();
            assertThat(service.errorRate()).isZero();
            assertThat(service.degraded()).isFalse();
        });
    }

    @Test
    void shouldComputeRatesAndAverage_whenLogsExist() {
        InMemoryLogRepository repository = new InMemoryLogRepository(100);
        repository.save(success("payment-service", 100));
        repository.save(success("payment-service", 200));
        repository.save(failure("payment-service", 300));

        MetricsSummary summary = metricsService(repository).summarize();
        ServiceMetrics payment = service(summary, "payment-service");

        assertThat(payment.totalCalls()).isEqualTo(3);
        assertThat(payment.successCalls()).isEqualTo(2);
        assertThat(payment.errorCalls()).isEqualTo(1);
        assertThat(payment.errorRate()).isEqualTo(33.33);
        assertThat(payment.successRate()).isEqualTo(66.67);
        assertThat(payment.avgResponseTimeMs()).isEqualTo(200.0);
        assertThat(payment.degraded()).isTrue();
    }

    @Test
    void shouldNotBeDegraded_whenErrorRateIsExactlyFifteenPercent() {
        InMemoryLogRepository repository = new InMemoryLogRepository(100);
        for (int index = 0; index < 17; index++) {
            repository.save(success("order-service", 50));
        }
        for (int index = 0; index < 3; index++) {
            repository.save(failure("order-service", 50));
        }

        MetricsSummary summary = metricsService(repository).summarize();

        assertThat(service(summary, "order-service").errorRate()).isEqualTo(15.0);
        assertThat(service(summary, "order-service").degraded()).isFalse();
    }

    private MetricsService metricsService(InMemoryLogRepository repository) {
        return new MetricsService(repository, properties());
    }

    private ServiceMetrics service(MetricsSummary summary, String serviceId) {
        return summary.services().stream()
                .filter(service -> service.serviceId().equals(serviceId))
                .findFirst()
                .orElseThrow();
    }

    private AppProperties properties() {
        return new AppProperties(
                new AppProperties.Cors(List.of("http://localhost:5173")),
                new AppProperties.Simulation(0.0, 0, 0, 50, 4),
                new AppProperties.Logging(1000, 5),
                new AppProperties.Metrics(15.0),
                new AppProperties.Pagination(20, 100));
    }

    private LogEntry success(String serviceId, long durationMs) {
        return LogEntry.success("req-" + Math.random(), serviceId, "list", durationMs, BASE, List.of(), Map.of("ok", true));
    }

    private LogEntry failure(String serviceId, long durationMs) {
        return LogEntry.failure("req-" + Math.random(), serviceId, "reserve", durationMs, BASE, List.of(), "boom",
                List.of("boom"));
    }
}
