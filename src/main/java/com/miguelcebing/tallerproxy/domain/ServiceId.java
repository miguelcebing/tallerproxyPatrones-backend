package com.miguelcebing.tallerproxy.domain;

import com.miguelcebing.tallerproxy.exception.InvalidRequestException;

public enum ServiceId {

    INVENTORY("inventory-service"),
    ORDERS("order-service"),
    PAYMENTS("payment-service");

    private final String id;

    ServiceId(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ServiceId fromId(String id) {
        for (ServiceId serviceId : values()) {
            if (serviceId.id.equalsIgnoreCase(id)) {
                return serviceId;
            }
        }
        throw new InvalidRequestException("Unknown service id: " + id);
    }
}
