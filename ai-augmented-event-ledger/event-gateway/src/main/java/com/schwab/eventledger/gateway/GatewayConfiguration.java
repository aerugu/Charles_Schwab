package com.schwab.eventledger.gateway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({AccountServiceProperties.class, RateLimitProperties.class, PendingEventRetryProperties.class})
class GatewayConfiguration {
    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173")
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("X-Trace-Id");
            }
        };
    }
}
