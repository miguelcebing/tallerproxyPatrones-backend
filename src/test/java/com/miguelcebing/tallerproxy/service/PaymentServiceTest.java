package com.miguelcebing.tallerproxy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miguelcebing.tallerproxy.exception.ResourceNotFoundException;
import com.miguelcebing.tallerproxy.exception.SimulatedFailureException;
import com.miguelcebing.tallerproxy.exception.UnknownOperationException;
import com.miguelcebing.tallerproxy.simulation.LatencySimulator;
import com.miguelcebing.tallerproxy.simulation.RandomFailureInjector;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private static final LatencySimulator NO_LATENCY = new LatencySimulator(0, 0);

    @Test
    void shouldThrowSimulatedFailure_whenInjectorAlwaysFails() {
        PaymentService service = new PaymentService(new RandomFailureInjector(0.10, () -> 0.0), NO_LATENCY);

        assertThatThrownBy(() -> service.execute("charge", Map.of("orderId", "ORD-1", "amount", 10.0)))
                .isInstanceOf(SimulatedFailureException.class);
    }

    @Test
    void shouldApprovePayment_whenInjectorNeverFails() {
        PaymentService service = new PaymentService(new RandomFailureInjector(0.10, () -> 0.99), NO_LATENCY);

        Map<String, Object> payment = service.execute("charge", Map.of("orderId", "ORD-1", "amount", 120.5));

        assertThat(payment).containsEntry("status", "APPROVED").containsEntry("orderId", "ORD-1");
        assertThat(payment.get("paymentId")).asString().startsWith("PAY-");
    }

    @Test
    void shouldThrowNotFound_whenPaymentDoesNotExist() {
        PaymentService service = new PaymentService(new RandomFailureInjector(0.0, () -> 0.99), NO_LATENCY);

        assertThatThrownBy(() -> service.execute("status", Map.of("paymentId", "PAY-404")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowUnknownOperation_whenOperationIsNotSupported() {
        PaymentService service = new PaymentService(new RandomFailureInjector(0.0, () -> 0.99), NO_LATENCY);

        assertThatThrownBy(() -> service.execute("teleport", Map.of()))
                .isInstanceOf(UnknownOperationException.class);
    }
}
