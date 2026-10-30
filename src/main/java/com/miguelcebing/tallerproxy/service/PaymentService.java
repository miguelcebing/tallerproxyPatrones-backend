package com.miguelcebing.tallerproxy.service;

import com.miguelcebing.tallerproxy.domain.ServiceId;
import com.miguelcebing.tallerproxy.exception.BusinessRuleException;
import com.miguelcebing.tallerproxy.exception.ResourceNotFoundException;
import com.miguelcebing.tallerproxy.exception.UnknownOperationException;
import com.miguelcebing.tallerproxy.proxy.MicroserviceProxy;
import com.miguelcebing.tallerproxy.simulation.FailureInjector;
import com.miguelcebing.tallerproxy.simulation.LatencySimulator;
import com.miguelcebing.tallerproxy.support.ParamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentService implements MicroserviceProxy<Map<String, Object>> {

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REFUNDED = "REFUNDED";

    private final FailureInjector failureInjector;
    private final LatencySimulator latencySimulator;
    private final ConcurrentHashMap<String, Map<String, Object>> payments = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public PaymentService(FailureInjector failureInjector, LatencySimulator latencySimulator) {
        this.failureInjector = failureInjector;
        this.latencySimulator = latencySimulator;
    }

    @Override
    public Map<String, Object> execute(String operation, Object... params) {
        latencySimulator.simulate();
        failureInjector.maybeFail(ServiceId.PAYMENTS.id(), operation);
        Map<String, Object> input = ParamReader.asMap(params);
        return switch (operation) {
            case "charge" -> charge(input);
            case "refund" -> refund(input);
            case "status" -> status(input);
            default -> throw new UnknownOperationException(ServiceId.PAYMENTS.id(), operation);
        };
    }

    private Map<String, Object> charge(Map<String, Object> input) {
        String orderId = ParamReader.requireString(input, "orderId");
        double amount = ParamReader.requirePositiveNumber(input, "amount");

        String paymentId = "PAY-" + sequence.incrementAndGet();
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("paymentId", paymentId);
        payment.put("orderId", orderId);
        payment.put("amount", amount);
        payment.put("status", STATUS_APPROVED);
        payments.put(paymentId, Map.copyOf(payment));
        return payment;
    }

    private Map<String, Object> refund(Map<String, Object> input) {
        String paymentId = ParamReader.requireString(input, "paymentId");
        Map<String, Object> payment = findPayment(paymentId);
        if (STATUS_REFUNDED.equals(payment.get("status"))) {
            throw new BusinessRuleException("Payment " + paymentId + " is already refunded");
        }
        Map<String, Object> refunded = new LinkedHashMap<>(payment);
        refunded.put("status", STATUS_REFUNDED);
        payments.put(paymentId, Map.copyOf(refunded));
        return refunded;
    }

    private Map<String, Object> status(Map<String, Object> input) {
        return findPayment(ParamReader.requireString(input, "paymentId"));
    }

    private Map<String, Object> findPayment(String paymentId) {
        Map<String, Object> payment = payments.get(paymentId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found: " + paymentId);
        }
        return payment;
    }
}
