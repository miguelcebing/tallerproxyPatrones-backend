package com.miguelcebing.tallerproxy.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miguelcebing.tallerproxy.logging.LogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.simulation.payment-failure-rate=0",
        "app.simulation.min-latency-ms=0",
        "app.simulation.max-latency-ms=0"
})
@AutoConfigureMockMvc
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogRepository logRepository;

    @BeforeEach
    void clearLogs() {
        logRepository.clear();
    }

    @Test
    void shouldReturnAllServices_whenSummaryIsRequested() throws Exception {
        mockMvc.perform(get("/api/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalls").value(0))
                .andExpect(jsonPath("$.services.length()").value(3))
                .andExpect(jsonPath("$.services[0].degraded").value(false));
    }

    @Test
    void shouldGenerateFiftyCalls_whenSimulateLoadIsRequested() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/metrics/simulate-load"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(50))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"requested\":50");
        assertThat(logRepository.count()).isGreaterThanOrEqualTo(50);
    }

    @Test
    void shouldPaginateLogs_whenDefaultSizeIsUsed() throws Exception {
        mockMvc.perform(post("/api/metrics/simulate-load")).andExpect(status().isOk());

        mockMvc.perform(get("/api/metrics/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.content.length()").value(20));
    }

    @Test
    void shouldReturnOnlyErrors_whenStatusFilterIsUsed() throws Exception {
        mockMvc.perform(post("/api/metrics/simulate-load")).andExpect(status().isOk());

        mockMvc.perform(get("/api/metrics/logs?status=ERROR&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ERROR"));
    }

    @Test
    void shouldReturnBadRequest_whenSizeExceedsMaximum() throws Exception {
        mockMvc.perform(get("/api/metrics/logs?size=101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_whenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/metrics/logs?status=WHATEVER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_whenServiceIsUnknown() throws Exception {
        mockMvc.perform(get("/api/metrics/logs?service=shipping-service"))
                .andExpect(status().isBadRequest());
    }
}
