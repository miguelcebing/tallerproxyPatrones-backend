package com.miguelcebing.tallerproxy.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miguelcebing.tallerproxy.logging.LogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.simulation.payment-failure-rate=0",
        "app.simulation.min-latency-ms=0",
        "app.simulation.max-latency-ms=0"
})
@AutoConfigureMockMvc
class ServiceGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogRepository logRepository;

    @BeforeEach
    void clearLogs() {
        logRepository.clear();
    }

    @Test
    void shouldReturnInventoryList_whenOperationIsValid() throws Exception {
        mockMvc.perform(post("/api/services/inventory/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void shouldReturnBadRequest_whenOperationNameIsInvalid() throws Exception {
        mockMvc.perform(post("/api/services/inventory/LIST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void shouldReturnNotFound_whenOperationIsUnknown() throws Exception {
        mockMvc.perform(post("/api/services/inventory/teleport"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Unknown operation"));
    }

    @Test
    void shouldReturnBadRequest_whenRequiredParameterIsMissing() throws Exception {
        mockMvc.perform(post("/api/services/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnprocessableEntity_whenStockIsInsufficient() throws Exception {
        mockMvc.perform(post("/api/services/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-003\",\"quantity\":5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business rule violated"));
    }

    @Test
    void shouldChargePayment_whenFailureRateIsZero() throws Exception {
        mockMvc.perform(post("/api/services/payments/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"ORD-1\",\"amount\":120.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldCreateOrderAndLogInventoryCascade_whenOrderIsCreated() throws Exception {
        mockMvc.perform(post("/api/services/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/metrics/logs?size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].serviceId").value("order-service"))
                .andExpect(jsonPath("$.content[1].serviceId").value("inventory-service"));
    }

    @Test
    void shouldReturnNotFound_whenServiceDomainIsUnknown() throws Exception {
        mockMvc.perform(post("/api/services/shipping/list"))
                .andExpect(status().isNotFound());
    }
}
