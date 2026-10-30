package com.miguelcebing.tallerproxy.domain;

import java.time.Instant;
import java.util.List;

public record LogEntry(
        String requestId,
        String serviceId,
        String operation,
        long durationMs,
        CallStatus status,
        Instant timestamp,
        List<Object> input,
        Object response,
        String errorMessage,
        List<String> stackTrace) {

    public static LogEntry success(String requestId,
                                   String serviceId,
                                   String operation,
                                   long durationMs,
                                   Instant timestamp,
                                   List<Object> input,
                                   Object response) {
        return new LogEntry(requestId, serviceId, operation, durationMs, CallStatus.SUCCESS, timestamp,
                List.copyOf(input), response, null, List.of());
    }

    public static LogEntry failure(String requestId,
                                   String serviceId,
                                   String operation,
                                   long durationMs,
                                   Instant timestamp,
                                   List<Object> input,
                                   String errorMessage,
                                   List<String> stackTrace) {
        return new LogEntry(requestId, serviceId, operation, durationMs, CallStatus.ERROR, timestamp,
                List.copyOf(input), null, errorMessage, List.copyOf(stackTrace));
    }
}
