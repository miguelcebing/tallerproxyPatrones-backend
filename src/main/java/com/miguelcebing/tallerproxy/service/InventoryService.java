package com.miguelcebing.tallerproxy.service;

import com.miguelcebing.tallerproxy.domain.ServiceId;
import com.miguelcebing.tallerproxy.exception.BusinessRuleException;
import com.miguelcebing.tallerproxy.exception.ResourceNotFoundException;
import com.miguelcebing.tallerproxy.exception.UnknownOperationException;
import com.miguelcebing.tallerproxy.proxy.MicroserviceProxy;
import com.miguelcebing.tallerproxy.simulation.LatencySimulator;
import com.miguelcebing.tallerproxy.support.ParamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryService implements MicroserviceProxy<Map<String, Object>> {

    private final LatencySimulator latencySimulator;
    private final ConcurrentHashMap<String, AtomicInteger> stock = new ConcurrentHashMap<>();

    public InventoryService(LatencySimulator latencySimulator) {
        this.latencySimulator = latencySimulator;
        stock.put("SKU-001", new AtomicInteger(25));
        stock.put("SKU-002", new AtomicInteger(10));
        stock.put("SKU-003", new AtomicInteger(0));
    }

    @Override
    public Map<String, Object> execute(String operation, Object... params) {
        latencySimulator.simulate();
        Map<String, Object> input = ParamReader.asMap(params);
        return switch (operation) {
            case "list" -> list();
            case "check-stock" -> checkStock(input);
            case "reserve" -> reserve(input);
            case "release" -> release(input);
            case "restock" -> restock(input);
            default -> throw new UnknownOperationException(ServiceId.INVENTORY.id(), operation);
        };
    }

    private Map<String, Object> list() {
        List<Map<String, Object>> items = new ArrayList<>();
        stock.keySet().stream().sorted().forEach(sku ->
                items.add(Map.of("sku", sku, "available", stock.get(sku).get())));
        return Map.of("items", items, "count", items.size());
    }

    private Map<String, Object> checkStock(Map<String, Object> input) {
        String sku = ParamReader.requireString(input, "sku");
        AtomicInteger available = stock.get(sku);
        if (available == null) {
            throw new ResourceNotFoundException("Unknown sku: " + sku);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sku", sku);
        result.put("available", available.get());
        return result;
    }

    private Map<String, Object> reserve(Map<String, Object> input) {
        String sku = ParamReader.requireString(input, "sku");
        int quantity = ParamReader.requirePositiveInt(input, "quantity");
        AtomicInteger available = stock.computeIfAbsent(sku, key -> new AtomicInteger(0));
        int remaining = decrementIfAvailable(available, quantity, sku);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sku", sku);
        result.put("reserved", quantity);
        result.put("available", remaining);
        return result;
    }

    private Map<String, Object> release(Map<String, Object> input) {
        String sku = ParamReader.requireString(input, "sku");
        int quantity = ParamReader.requirePositiveInt(input, "quantity");
        AtomicInteger available = stock.computeIfAbsent(sku, key -> new AtomicInteger(0));
        int remaining = available.addAndGet(quantity);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sku", sku);
        result.put("released", quantity);
        result.put("available", remaining);
        return result;
    }

    private Map<String, Object> restock(Map<String, Object> input) {
        String sku = ParamReader.requireString(input, "sku");
        int quantity = ParamReader.requirePositiveInt(input, "quantity");
        AtomicInteger available = stock.computeIfAbsent(sku, key -> new AtomicInteger(0));
        int remaining = available.addAndGet(quantity);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sku", sku);
        result.put("restocked", quantity);
        result.put("available", remaining);
        return result;
    }

    private int decrementIfAvailable(AtomicInteger available, int quantity, String sku) {
        while (true) {
            int current = available.get();
            if (current < quantity) {
                throw new BusinessRuleException("Insufficient stock for " + sku
                        + ": requested " + quantity + ", available " + current);
            }
            if (available.compareAndSet(current, current - quantity)) {
                return current - quantity;
            }
        }
    }
}
