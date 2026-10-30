package com.miguelcebing.tallerproxy.simulation;

public interface FailureInjector {

    void maybeFail(String serviceId, String operation);
}
