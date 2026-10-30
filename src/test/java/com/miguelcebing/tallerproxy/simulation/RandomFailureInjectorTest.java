package com.miguelcebing.tallerproxy.simulation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miguelcebing.tallerproxy.exception.SimulatedFailureException;
import org.junit.jupiter.api.Test;

class RandomFailureInjectorTest {

    @Test
    void shouldFail_whenRandomValueIsBelowRate() {
        FailureInjector injector = new RandomFailureInjector(0.10, () -> 0.05);

        assertThatThrownBy(() -> injector.maybeFail("payment-service", "charge"))
                .isInstanceOf(SimulatedFailureException.class)
                .hasMessageContaining("payment-service");
    }

    @Test
    void shouldNotFail_whenRandomValueIsAboveRate() {
        FailureInjector injector = new RandomFailureInjector(0.10, () -> 0.95);

        assertThatCode(() -> injector.maybeFail("payment-service", "charge"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNeverFail_whenRateIsZero() {
        FailureInjector injector = new RandomFailureInjector(0.0, () -> 0.0);

        assertThatCode(() -> injector.maybeFail("payment-service", "charge"))
                .doesNotThrowAnyException();
    }
}
