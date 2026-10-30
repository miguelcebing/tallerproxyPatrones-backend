package com.miguelcebing.tallerproxy.api;

import com.miguelcebing.tallerproxy.exception.InvalidRequestException;
import com.miguelcebing.tallerproxy.exception.ResourceNotFoundException;
import com.miguelcebing.tallerproxy.proxy.MicroserviceProxy;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class ServiceGatewayController {

    private static final Pattern OPERATION_PATTERN = Pattern.compile("^[a-z0-9-]{1,40}$");

    private final Map<String, MicroserviceProxy<Map<String, Object>>> proxies;

    public ServiceGatewayController(
            @Qualifier("inventoryProxy") MicroserviceProxy<Map<String, Object>> inventoryProxy,
            @Qualifier("orderProxy") MicroserviceProxy<Map<String, Object>> orderProxy,
            @Qualifier("paymentProxy") MicroserviceProxy<Map<String, Object>> paymentProxy) {
        this.proxies = Map.of(
                "inventory", inventoryProxy,
                "orders", orderProxy,
                "payments", paymentProxy);
    }

    @PostMapping("/{domain}/{operation}")
    public Map<String, Object> invoke(@PathVariable String domain,
                                      @PathVariable String operation,
                                      @RequestBody(required = false) Map<String, Object> body) {
        MicroserviceProxy<Map<String, Object>> proxy = proxies.get(domain);
        if (proxy == null) {
            throw new ResourceNotFoundException("Unknown service: " + domain);
        }
        if (!OPERATION_PATTERN.matcher(operation).matches()) {
            throw new InvalidRequestException("Invalid operation name: " + operation);
        }
        return proxy.execute(operation, body == null ? Map.of() : body);
    }
}
