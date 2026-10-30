package com.miguelcebing.tallerproxy.simulation;

import java.util.concurrent.ThreadLocalRandom;

public class LatencySimulator {

    private final long minLatencyMs;
    private final long maxLatencyMs;

    public LatencySimulator(long minLatencyMs, long maxLatencyMs) {
        this.minLatencyMs = Math.max(0, minLatencyMs);
        this.maxLatencyMs = Math.max(this.minLatencyMs, maxLatencyMs);
    }

    public void simulate() {
        if (maxLatencyMs <= 0) {
            return;
        }
        long bound = maxLatencyMs - minLatencyMs + 1;
        long delay = minLatencyMs + ThreadLocalRandom.current().nextLong(bound);
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
