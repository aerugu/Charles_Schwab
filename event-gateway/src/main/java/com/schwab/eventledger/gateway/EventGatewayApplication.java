package com.schwab.eventledger.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the public Event Gateway process.
 */
@SpringBootApplication
public class EventGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventGatewayApplication.class, args);
    }
}
