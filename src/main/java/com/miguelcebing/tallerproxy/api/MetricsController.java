package com.miguelcebing.tallerproxy.api;

import com.miguelcebing.tallerproxy.config.AppProperties;
import com.miguelcebing.tallerproxy.domain.CallStatus;
import com.miguelcebing.tallerproxy.domain.LogEntry;
import com.miguelcebing.tallerproxy.domain.LogQuery;
import com.miguelcebing.tallerproxy.domain.MetricsSummary;
import com.miguelcebing.tallerproxy.domain.PageResult;
import com.miguelcebing.tallerproxy.domain.ServiceId;
import com.miguelcebing.tallerproxy.exception.InvalidRequestException;
import com.miguelcebing.tallerproxy.logging.LogRepository;
import com.miguelcebing.tallerproxy.metrics.MetricsService;
import com.miguelcebing.tallerproxy.simulation.LoadSimulator;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;
    private final LogRepository logRepository;
    private final LoadSimulator loadSimulator;
    private final AppProperties properties;

    public MetricsController(MetricsService metricsService,
                             LogRepository logRepository,
                             LoadSimulator loadSimulator,
                             AppProperties properties) {
        this.metricsService = metricsService;
        this.logRepository = logRepository;
        this.loadSimulator = loadSimulator;
        this.properties = properties;
    }

    @GetMapping("/summary")
    public MetricsSummary summary() {
        return metricsService.summarize();
    }

    @GetMapping("/logs")
    public PageResult<LogEntry> logs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        if (page < 0) {
            throw new InvalidRequestException("Parameter 'page' must be zero or greater");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("Parameter 'from' must be before 'to'");
        }
        LogQuery query = new LogQuery(resolveService(service), resolveStatus(status), from, to, page, resolveSize(size));
        return logRepository.find(query);
    }

    @PostMapping("/simulate-load")
    public Map<String, Object> simulateLoad() {
        return loadSimulator.simulate();
    }

    private String resolveService(String service) {
        if (service == null || service.isBlank()) {
            return null;
        }
        return ServiceId.fromId(service).id();
    }

    private CallStatus resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return CallStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Parameter 'status' must be SUCCESS or ERROR");
        }
    }

    private int resolveSize(Integer size) {
        int maxSize = properties.pagination().maxSize();
        int resolved = size == null ? properties.pagination().defaultSize() : size;
        if (resolved < 1 || resolved > maxSize) {
            throw new InvalidRequestException("Parameter 'size' must be between 1 and " + maxSize);
        }
        return resolved;
    }
}
