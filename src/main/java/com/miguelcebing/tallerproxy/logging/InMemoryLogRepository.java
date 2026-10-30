package com.miguelcebing.tallerproxy.logging;

import com.miguelcebing.tallerproxy.domain.LogEntry;
import com.miguelcebing.tallerproxy.domain.LogQuery;
import com.miguelcebing.tallerproxy.domain.PageResult;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class InMemoryLogRepository implements LogRepository {

    private final int capacity;
    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private final Object lock = new Object();

    public InMemoryLogRepository(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Log capacity must be positive");
        }
        this.capacity = capacity;
    }

    @Override
    public void save(LogEntry entry) {
        synchronized (lock) {
            if (entries.size() == capacity) {
                entries.removeLast();
            }
            entries.addFirst(entry);
        }
    }

    @Override
    public List<LogEntry> findAll() {
        synchronized (lock) {
            return List.copyOf(entries);
        }
    }

    @Override
    public PageResult<LogEntry> find(LogQuery query) {
        List<LogEntry> matches = new ArrayList<>();
        for (LogEntry entry : findAll()) {
            if (matches(entry, query)) {
                matches.add(entry);
            }
        }
        int fromIndex = Math.min(query.page() * query.size(), matches.size());
        int toIndex = Math.min(fromIndex + query.size(), matches.size());
        return PageResult.of(matches.subList(fromIndex, toIndex), query.page(), query.size(), matches.size());
    }

    @Override
    public long count() {
        synchronized (lock) {
            return entries.size();
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            entries.clear();
        }
    }

    private boolean matches(LogEntry entry, LogQuery query) {
        if (query.serviceId() != null && !query.serviceId().equals(entry.serviceId())) {
            return false;
        }
        if (query.status() != null && query.status() != entry.status()) {
            return false;
        }
        return withinRange(entry.timestamp(), query.from(), query.to());
    }

    private boolean withinRange(Instant timestamp, Instant from, Instant to) {
        if (from != null && timestamp.isBefore(from)) {
            return false;
        }
        return to == null || !timestamp.isAfter(to);
    }

    public int capacity() {
        return capacity;
    }
}
