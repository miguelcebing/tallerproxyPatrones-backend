package com.miguelcebing.tallerproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TallerProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TallerProxyApplication.class, args);
    }
}
