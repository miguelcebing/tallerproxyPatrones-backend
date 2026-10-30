package com.miguelcebing.tallerproxy.simulation;

import com.miguelcebing.tallerproxy.config.AppProperties;
import com.miguelcebing.tallerproxy.proxy.MicroserviceProxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LoadSimulator {

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 15;

    private final Map<String, MicroserviceProxy<Map<String, Object>>> proxies;
    private final List<SimulatedCall> calls;
    private final int requestedCalls;
    private final int poolSize;

    public LoadSimulator(@Qualifier("inventoryProxy") MicroserviceProxy<Map<String, Object>> inventoryProxy,
                         @Qualifier("orderProxy") MicroserviceProxy<Map<String, Object>> orderProxy,
                         @Qualifier("paymentProxy") MicroserviceProxy<Map<String, Object>> paymentProxy,
                         AppProperties properties) {
        this.proxies = Map.of(
                "inventory-service", inventoryProxy,
                "order-service", orderProxy,
                "payment-service", paymentProxy);
        this.calls = buildCalls();
        this.requestedCalls = properties.simulation().loadRequests();
        this.poolSize = Math.max(1, Math.min(properties.simulation().loadThreads(), requestedCalls));
    }

    public Map<String, Object> simulate() {
        AtomicInteger successful = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
            List<Future<?>> futures = new ArrayList<>(requestedCalls);
            for (int index = 0; index < requestedCalls; index++) {
                SimulatedCall call = randomCall();
                futures.add(executor.submit(() -> runCall(call, successful, failed)));
            }
            awaitAll(futures, failed);
        } finally {
            shutdown(executor);
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("requested", requestedCalls);
        summary.put("successful", successful.get());
        summary.put("failed", failed.get());
        return summary;
    }

    private void runCall(SimulatedCall call, AtomicInteger successful, AtomicInteger failed) {
        try {
            proxies.get(call.serviceId()).execute(call.operation(), call.params());
            successful.incrementAndGet();
        } catch (RuntimeException ex) {
            failed.incrementAndGet();
        }
    }

    private void awaitAll(List<Future<?>> futures, AtomicInteger failed) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException ex) {
                failed.incrementAndGet();
            }
        }
    }

    private void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private SimulatedCall randomCall() {
        return calls.get(ThreadLocalRandom.current().nextInt(calls.size()));
    }

    private List<SimulatedCall> buildCalls() {
        List<SimulatedCall> simulatedCalls = new ArrayList<>();
        simulatedCalls.add(new SimulatedCall("inventory-service", "list", Map.of()));
        simulatedCalls.add(new SimulatedCall("inventory-service", "check-stock", Map.of("sku", "SKU-001")));
        simulatedCalls.add(new SimulatedCall("inventory-service", "reserve", Map.of("sku", "SKU-001", "quantity", 1)));
        simulatedCalls.add(new SimulatedCall("inventory-service", "restock", Map.of("sku", "SKU-002", "quantity", 3)));
        simulatedCalls.add(new SimulatedCall("inventory-service", "check-stock", Map.of("sku", "SKU-999")));
        simulatedCalls.add(new SimulatedCall("inventory-service", "reserve", Map.of("sku", "SKU-003", "quantity", 4)));
        simulatedCalls.add(new SimulatedCall("inventory-service", "teleport", Map.of()));
        simulatedCalls.add(new SimulatedCall("order-service", "create", Map.of("sku", "SKU-002", "quantity", 1)));
        simulatedCalls.add(new SimulatedCall("order-service", "create", Map.of("sku", "SKU-001", "quantity", 2)));
        simulatedCalls.add(new SimulatedCall("order-service", "list", Map.of()));
        simulatedCalls.add(new SimulatedCall("order-service", "get", Map.of("orderId", "ORD-1")));
        simulatedCalls.add(new SimulatedCall("order-service", "cancel", Map.of("orderId", "ORD-1")));
        simulatedCalls.add(new SimulatedCall("payment-service", "charge", Map.of("orderId", "ORD-1", "amount", 120.5)));
        simulatedCalls.add(new SimulatedCall("payment-service", "charge", Map.of("orderId", "ORD-2", "amount", 49.99)));
        simulatedCalls.add(new SimulatedCall("payment-service", "status", Map.of("paymentId", "PAY-1")));
        simulatedCalls.add(new SimulatedCall("payment-service", "refund", Map.of("paymentId", "PAY-1")));
        return List.copyOf(simulatedCalls);
    }

    private record SimulatedCall(String serviceId, String operation, Map<String, Object> params) {
    }
}
