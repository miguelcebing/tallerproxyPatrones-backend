package com.miguelcebing.tallerproxy.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miguelcebing.tallerproxy.domain.CallStatus;
import com.miguelcebing.tallerproxy.domain.LogEntry;
import com.miguelcebing.tallerproxy.exception.BusinessRuleException;
import com.miguelcebing.tallerproxy.logging.InMemoryLogRepository;
import com.miguelcebing.tallerproxy.logging.SensitiveDataMasker;
import com.miguelcebing.tallerproxy.logging.StackTraceSummarizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoggingProxyTest {

    private static final String SERVICE_ID = "inventory-service";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-10-30T15:04:05Z");

    private final InMemoryLogRepository repository = new InMemoryLogRepository(50);
    private final SensitiveDataMasker masker = new SensitiveDataMasker();
    private final StackTraceSummarizer summarizer = new StackTraceSummarizer(5);
    private final Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Test
    void shouldLogSuccessWithRequestIdAndDuration_whenTargetSucceeds() {
        MicroserviceProxy<Map<String, Object>> target =
                (operation, params) -> Map.<String, Object>of("ok", true);
        LoggingProxy<Map<String, Object>> proxy = proxyFor(target);

        Map<String, Object> result = proxy.execute("list", Map.of("sku", "SKU-001"));

        assertThat(result).containsEntry("ok", true);
        LogEntry entry = repository.findAll().getFirst();
        assertThat(entry.status()).isEqualTo(CallStatus.SUCCESS);
        assertThat(entry.serviceId()).isEqualTo(SERVICE_ID);
        assertThat(entry.operation()).isEqualTo("list");
        assertThat(entry.timestamp()).isEqualTo(FIXED_INSTANT);
        assertThat(entry.durationMs()).isGreaterThanOrEqualTo(0);
        assertThat(entry.response()).isEqualTo(Map.of("ok", true));
        assertThat(entry.errorMessage()).isNull();
        assertThat(UUID.fromString(entry.requestId())).isNotNull();
    }

    @Test
    void shouldLogErrorWithStackTraceAndRethrow_whenTargetFails() {
        RuntimeException failure = new BusinessRuleException("Insufficient stock");
        MicroserviceProxy<Map<String, Object>> target = (operation, params) -> {
            throw failure;
        };
        LoggingProxy<Map<String, Object>> proxy = proxyFor(target);

        assertThatThrownBy(() -> proxy.execute("reserve", Map.of("sku", "SKU-001", "quantity", 99)))
                .isSameAs(failure);

        LogEntry entry = repository.findAll().getFirst();
        assertThat(entry.status()).isEqualTo(CallStatus.ERROR);
        assertThat(entry.errorMessage()).isEqualTo("Insufficient stock");
        assertThat(entry.stackTrace()).isNotEmpty();
        assertThat(entry.stackTrace().getFirst()).contains("BusinessRuleException");
        assertThat(entry.response()).isNull();
    }

    @Test
    void shouldMaskSensitiveParameters_whenLoggingInput() {
        MicroserviceProxy<Map<String, Object>> target =
                (operation, params) -> Map.<String, Object>of("charged", true);
        LoggingProxy<Map<String, Object>> proxy = proxyFor(target);

        proxy.execute("charge", Map.of("card", "4111111111111111", "amount", 10));

        Map<?, ?> loggedInput = (Map<?, ?>) repository.findAll().getFirst().input().getFirst();
        assertThat(loggedInput.get("card")).isEqualTo("***");
        assertThat(loggedInput.get("amount")).isEqualTo(10);
    }

    @Test
    void shouldKeepLoggedInputUnchanged_whenCallerMutatesParametersAfterwards() {
        MicroserviceProxy<Map<String, Object>> target =
                (operation, params) -> Map.<String, Object>of("ok", true);
        LoggingProxy<Map<String, Object>> proxy = proxyFor(target);
        Map<String, Object> params = new HashMap<>();
        params.put("sku", "SKU-001");

        proxy.execute("check-stock", params);
        params.put("sku", "MUTATED");

        Map<?, ?> loggedInput = (Map<?, ?>) repository.findAll().getFirst().input().getFirst();
        assertThat(loggedInput.get("sku")).isEqualTo("SKU-001");
    }

    private LoggingProxy<Map<String, Object>> proxyFor(MicroserviceProxy<Map<String, Object>> target) {
        return new LoggingProxy<>(SERVICE_ID, target, repository, clock, masker, summarizer);
    }
}
