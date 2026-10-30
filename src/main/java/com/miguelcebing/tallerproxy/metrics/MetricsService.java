package com.miguelcebing.tallerproxy.metrics;

import com.miguelcebing.tallerproxy.config.AppProperties;
import com.miguelcebing.tallerproxy.domain.CallStatus;
import com.miguelcebing.tallerproxy.domain.LogEntry;
import com.miguelcebing.tallerproxy.domain.MetricsSummary;
import com.miguelcebing.tallerproxy.domain.ServiceId;
import com.miguelcebing.tallerproxy.domain.ServiceMetrics;
import com.miguelcebing.tallerproxy.logging.LogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private static final double PERCENT = 100.0;
    private static final int SCALE = 2;

    private final LogRepository logRepository;
    private final double degradedErrorRateThreshold;

    public MetricsService(LogRepository logRepository, AppProperties properties) {
        this.logRepository = logRepository;
        this.degradedErrorRateThreshold = properties.metrics().degradedErrorRateThreshold();
    }

    public MetricsSummary summarize() {
        List<LogEntry> entries = logRepository.findAll();
        List<ServiceMetrics> services = Arrays.stream(ServiceId.values())
                .map(serviceId -> summarizeService(serviceId.id(), entries))
                .toList();
        long totalCalls = entries.size();
        long errorCalls = entries.stream().filter(entry -> entry.status() == CallStatus.ERROR).count();
        return new MetricsSummary(totalCalls, errorCalls, percentage(errorCalls, totalCalls),
                averageDuration(entries), services);
    }

    private ServiceMetrics summarizeService(String serviceId, List<LogEntry> entries) {
        List<LogEntry> serviceEntries = entries.stream()
                .filter(entry -> entry.serviceId().equals(serviceId))
                .toList();
        long totalCalls = serviceEntries.size();
        long errorCalls = serviceEntries.stream().filter(entry -> entry.status() == CallStatus.ERROR).count();
        long successCalls = totalCalls - errorCalls;
        double errorRate = percentage(errorCalls, totalCalls);
        return new ServiceMetrics(serviceId, totalCalls, successCalls, errorCalls,
                percentage(successCalls, totalCalls), errorRate, averageDuration(serviceEntries),
                errorRate > degradedErrorRateThreshold);
    }

    private double percentage(long part, long total) {
        if (total == 0) {
            return 0.0;
        }
        return round(part * PERCENT / total);
    }

    private double averageDuration(List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return 0.0;
        }
        long total = entries.stream().mapToLong(LogEntry::durationMs).sum();
        return round((double) total / entries.size());
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
