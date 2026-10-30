package com.miguelcebing.tallerproxy.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(AppProperties properties) {
        this.allowedOrigins = properties.cors().allowedOrigins();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        if (allowedOrigins.contains("*")) {
            registration.allowedOriginPatterns("*");
        } else {
            registration.allowedOrigins(allowedOrigins.toArray(String[]::new));
        }
    }
}
