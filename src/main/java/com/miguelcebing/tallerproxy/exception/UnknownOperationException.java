package com.miguelcebing.tallerproxy.exception;

public class UnknownOperationException extends RuntimeException {

    public UnknownOperationException(String serviceId, String operation) {
        super("Unknown operation '" + operation + "' for " + serviceId);
    }
}
