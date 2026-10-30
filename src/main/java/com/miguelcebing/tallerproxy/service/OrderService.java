package com.miguelcebing.tallerproxy.service;

import com.miguelcebing.tallerproxy.domain.ServiceId;
import com.miguelcebing.tallerproxy.exception.BusinessRuleException;
import com.miguelcebing.tallerproxy.exception.ResourceNotFoundException;
import com.miguelcebing.tallerproxy.exception.UnknownOperationException;
import com.miguelcebing.tallerproxy.proxy.MicroserviceProxy;
import com.miguelcebing.tallerproxy.simulation.LatencySimulator;
import com.miguelcebing.tallerproxy.support.ParamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderService implements MicroserviceProxy<Map<String, Object>> {

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final MicroserviceProxy<Map<String, Object>> inventoryProxy;
    private final LatencySimulator latencySimulator;
    private final ConcurrentHashMap<String, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public OrderService(MicroserviceProxy<Map<String, Object>> inventoryProxy, LatencySimulator latencySimulator) {
        this.inventoryProxy = inventoryProxy;
        this.latencySimulator = latencySimulator;
    }

    @Override
    public Map<String, Object> execute(String operation, Object... params) {
        latencySimulator.simulate();
        Map<String, Object> input = ParamReader.asMap(params);
        return switch (operation) {
            case "create" -> create(input);
            case "get" -> get(input);
            case "list" -> list();
            case "cancel" -> cancel(input);
            default -> throw new UnknownOperationException(ServiceId.ORDERS.id(), operation);
        };
    }

    private Map<String, Object> create(Map<String, Object> input) {
        String sku = ParamReader.requireString(input, "sku");
        int quantity = ParamReader.requirePositiveInt(input, "quantity");

        inventoryProxy.execute("reserve", Map.of("sku", sku, "quantity", quantity));

        String orderId = "ORD-" + sequence.incrementAndGet();
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId", orderId);
        order.put("sku", sku);
        order.put("quantity", quantity);
        order.put("status", STATUS_CREATED);
        orders.put(orderId, Map.copyOf(order));
        return order;
    }

    private Map<String, Object> get(Map<String, Object> input) {
        String orderId = ParamReader.requireString(input, "orderId");
        return findOrder(orderId);
    }

    private Map<String, Object> list() {
        List<Map<String, Object>> content = List.copyOf(orders.values());
        return Map.of("orders", content, "count", content.size());
    }

    private Map<String, Object> cancel(Map<String, Object> input) {
        String orderId = ParamReader.requireString(input, "orderId");
        Map<String, Object> order = findOrder(orderId);
        if (STATUS_CANCELLED.equals(order.get("status"))) {
            throw new BusinessRuleException("Order " + orderId + " is already cancelled");
        }

        String sku = String.valueOf(order.get("sku"));
        int quantity = ((Number) order.get("quantity")).intValue();
        inventoryProxy.execute("release", Map.of("sku", sku, "quantity", quantity));

        Map<String, Object> cancelled = new LinkedHashMap<>(order);
        cancelled.put("status", STATUS_CANCELLED);
        orders.put(orderId, Map.copyOf(cancelled));
        return cancelled;
    }

    private Map<String, Object> findOrder(String orderId) {
        Map<String, Object> order = orders.get(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return order;
    }
}
