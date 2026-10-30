package com.miguelcebing.tallerproxy.exception;

public class SimulatedFailureException extends RuntimeException {

    public SimulatedFailureException(String serviceId, String operation) {
        super("Simulated failure in " + serviceId + " while executing '" + operation + "'");
    }
}
