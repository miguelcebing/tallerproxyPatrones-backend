package com.miguelcebing.tallerproxy.proxy;

import com.miguelcebing.tallerproxy.domain.LogEntry;
import com.miguelcebing.tallerproxy.logging.LogRepository;
import com.miguelcebing.tallerproxy.logging.SensitiveDataMasker;
import com.miguelcebing.tallerproxy.logging.StackTraceSummarizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingProxy<T> implements MicroserviceProxy<T> {

    private static final Logger log = LoggerFactory.getLogger(LoggingProxy.class);

    private final String serviceId;
    private final MicroserviceProxy<T> target;
    private final LogRepository repository;
    private final Clock clock;
    private final SensitiveDataMasker masker;
    private final StackTraceSummarizer stackTraceSummarizer;

    public LoggingProxy(String serviceId,
                        MicroserviceProxy<T> target,
                        LogRepository repository,
                        Clock clock,
                        SensitiveDataMasker masker,
                        StackTraceSummarizer stackTraceSummarizer) {
        this.serviceId = serviceId;
        this.target = target;
        this.repository = repository;
        this.clock = clock;
        this.masker = masker;
        this.stackTraceSummarizer = stackTraceSummarizer;
    }

    @Override
    public T execute(String operation, Object... params) {
        String requestId = UUID.randomUUID().toString();
        Instant timestamp = clock.instant();
        List<Object> maskedInput = masker.maskAll(params);
        log.info("START requestId={} service={} operation={}", requestId, serviceId, operation);

        long start = System.nanoTime();
        try {
            T result = target.execute(operation, params);
            long durationMs = elapsedMillis(start);
            repository.save(LogEntry.success(requestId, serviceId, operation, durationMs, timestamp,
                    maskedInput, masker.mask(result)));
            log.info("END requestId={} service={} operation={} status=SUCCESS durationMs={}",
                    requestId, serviceId, operation, durationMs);
            return result;
        } catch (RuntimeException ex) {
            long durationMs = elapsedMillis(start);
            repository.save(LogEntry.failure(requestId, serviceId, operation, durationMs, timestamp,
                    maskedInput, ex.getMessage(), stackTraceSummarizer.summarize(ex)));
            log.error("END requestId={} service={} operation={} status=ERROR durationMs={} error={}",
                    requestId, serviceId, operation, durationMs, ex.getMessage());
            throw ex;
        }
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
