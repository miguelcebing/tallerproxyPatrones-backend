package com.miguelcebing.tallerproxy.config;

import com.miguelcebing.tallerproxy.domain.ServiceId;
import com.miguelcebing.tallerproxy.logging.InMemoryLogRepository;
import com.miguelcebing.tallerproxy.logging.LogRepository;
import com.miguelcebing.tallerproxy.logging.SensitiveDataMasker;
import com.miguelcebing.tallerproxy.logging.StackTraceSummarizer;
import com.miguelcebing.tallerproxy.proxy.LoggingProxy;
import com.miguelcebing.tallerproxy.proxy.MicroserviceProxy;
import com.miguelcebing.tallerproxy.service.InventoryService;
import com.miguelcebing.tallerproxy.service.OrderService;
import com.miguelcebing.tallerproxy.service.PaymentService;
import com.miguelcebing.tallerproxy.simulation.FailureInjector;
import com.miguelcebing.tallerproxy.simulation.LatencySimulator;
import com.miguelcebing.tallerproxy.simulation.RandomFailureInjector;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SensitiveDataMasker sensitiveDataMasker() {
        return new SensitiveDataMasker();
    }

    @Bean
    public StackTraceSummarizer stackTraceSummarizer(AppProperties properties) {
        return new StackTraceSummarizer(properties.logging().stackTraceFrames());
    }

    @Bean
    public LogRepository logRepository(AppProperties properties) {
        return new InMemoryLogRepository(properties.logging().capacity());
    }

    @Bean
    public FailureInjector failureInjector(AppProperties properties) {
        return new RandomFailureInjector(properties.simulation().paymentFailureRate(),
                () -> ThreadLocalRandom.current().nextDouble());
    }

    @Bean
    public LatencySimulator latencySimulator(AppProperties properties) {
        return new LatencySimulator(properties.simulation().minLatencyMs(),
                properties.simulation().maxLatencyMs());
    }

    @Bean
    public MicroserviceProxy<Map<String, Object>> inventoryProxy(LogRepository repository,
                                                                 Clock clock,
                                                                 SensitiveDataMasker masker,
                                                                 StackTraceSummarizer summarizer,
                                                                 LatencySimulator latencySimulator) {
        InventoryService realService = new InventoryService(latencySimulator);
        return new LoggingProxy<>(ServiceId.INVENTORY.id(), realService, repository, clock, masker, summarizer);
    }

    @Bean
    public MicroserviceProxy<Map<String, Object>> orderProxy(
            @Qualifier("inventoryProxy") MicroserviceProxy<Map<String, Object>> inventoryProxy,
            LogRepository repository,
            Clock clock,
            SensitiveDataMasker masker,
            StackTraceSummarizer summarizer,
            LatencySimulator latencySimulator) {
        OrderService realService = new OrderService(inventoryProxy, latencySimulator);
        return new LoggingProxy<>(ServiceId.ORDERS.id(), realService, repository, clock, masker, summarizer);
    }

    @Bean
    public MicroserviceProxy<Map<String, Object>> paymentProxy(LogRepository repository,
                                                               Clock clock,
                                                               SensitiveDataMasker masker,
                                                               StackTraceSummarizer summarizer,
                                                               LatencySimulator latencySimulator,
                                                               FailureInjector failureInjector) {
        PaymentService realService = new PaymentService(failureInjector, latencySimulator);
        return new LoggingProxy<>(ServiceId.PAYMENTS.id(), realService, repository, clock, masker, summarizer);
    }
}
