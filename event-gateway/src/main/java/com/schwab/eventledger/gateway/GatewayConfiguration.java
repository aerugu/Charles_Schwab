package com.schwab.eventledger.gateway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AccountServiceProperties.class)
class GatewayConfiguration {
}
