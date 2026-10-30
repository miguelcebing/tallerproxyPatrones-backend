package com.miguelcebing.tallerproxy.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.miguelcebing.tallerproxy.domain.CallStatus;
import com.miguelcebing.tallerproxy.domain.LogEntry;
import com.miguelcebing.tallerproxy.domain.LogQuery;
import com.miguelcebing.tallerproxy.domain.PageResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryLogRepositoryTest {

    private static final Instant BASE = Instant.parse("2026-10-30T10:00:00Z");

    @Test
    void shouldReturnMostRecentFirst_whenSavingEntries() {
        InMemoryLogRepository repository = new InMemoryLogRepository(10);

        repository.save(success("inventory-service", BASE));
        repository.save(success("order-service", BASE.plusSeconds(1)));

        List<LogEntry> entries = repository.findAll();
        assertThat(entries).hasSize(2);
        assertThat(entries.getFirst().serviceId()).isEqualTo("order-service");
    }

    @Test
    void shouldFilterByServiceAndStatus_whenQuerying() {
        InMemoryLogRepository repository = new InMemoryLogRepository(10);
        repository.save(success("inventory-service", BASE));
        repository.save(failure("inventory-service", BASE.plusSeconds(1)));
        repository.save(failure("payment-service", BASE.plusSeconds(2)));

        PageResult<LogEntry> errors = repository.find(query("inventory-service", CallStatus.ERROR, null, null, 0, 20));
        PageResult<LogEntry> all = repository.find(query(null, null, null, null, 0, 20));

        assertThat(errors.content()).hasSize(1);
        assertThat(errors.content().getFirst().status()).isEqualTo(CallStatus.ERROR);
        assertThat(all.totalElements()).isEqualTo(3);
    }

    @Test
    void shouldFilterByDateRange_whenQuerying() {
        InMemoryLogRepository repository = new InMemoryLogRepository(10);
        repository.save(success("inventory-service", BASE));
        repository.save(success("inventory-service", BASE.plusSeconds(60)));
        repository.save(success("inventory-service", BASE.plusSeconds(120)));

        PageResult<LogEntry> result = repository.find(query(null, null, BASE.plusSeconds(30), BASE.plusSeconds(90), 0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().timestamp()).isEqualTo(BASE.plusSeconds(60));
    }

    @Test
    void shouldPaginateResults_whenQuerying() {
        InMemoryLogRepository repository = new InMemoryLogRepository(10);
        for (int index = 0; index < 5; index++) {
            repository.save(success("inventory-service", BASE.plusSeconds(index)));
        }

        PageResult<LogEntry> firstPage = repository.find(query(null, null, null, null, 0, 2));
        PageResult<LogEntry> lastPage = repository.find(query(null, null, null, null, 2, 2));

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(5);
        assertThat(firstPage.totalPages()).isEqualTo(3);
        assertThat(lastPage.content()).hasSize(1);
    }

    @Test
    void shouldEvictOldestEntry_whenCapacityIsReached() {
        InMemoryLogRepository repository = new InMemoryLogRepository(2);
        repository.save(success("inventory-service", BASE));
        repository.save(success("order-service", BASE.plusSeconds(1)));
        repository.save(success("payment-service", BASE.plusSeconds(2)));

        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.findAll())
                .extracting(LogEntry::serviceId)
                .containsExactly("payment-service", "order-service");
    }

    private LogQuery query(String serviceId, CallStatus status, Instant from, Instant to, int page, int size) {
        return new LogQuery(serviceId, status, from, to, page, size);
    }

    private LogEntry success(String serviceId, Instant timestamp) {
        return LogEntry.success("req-" + timestamp.toEpochMilli(), serviceId, "list", 10, timestamp,
                List.of(), java.util.Map.of("ok", true));
    }

    private LogEntry failure(String serviceId, Instant timestamp) {
        return LogEntry.failure("req-" + timestamp.toEpochMilli(), serviceId, "reserve", 12, timestamp,
                List.of(), "boom", List.of("boom"));
    }
}
