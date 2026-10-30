package com.miguelcebing.tallerproxy.simulation;

import com.miguelcebing.tallerproxy.exception.SimulatedFailureException;
import java.util.function.DoubleSupplier;

public class RandomFailureInjector implements FailureInjector {

    private final double failureRate;
    private final DoubleSupplier randomSource;

    public RandomFailureInjector(double failureRate, DoubleSupplier randomSource) {
        this.failureRate = failureRate;
        this.randomSource = randomSource;
    }

    @Override
    public void maybeFail(String serviceId, String operation) {
        if (failureRate <= 0) {
            return;
        }
        if (randomSource.getAsDouble() < failureRate) {
            throw new SimulatedFailureException(serviceId, operation);
        }
    }
}
