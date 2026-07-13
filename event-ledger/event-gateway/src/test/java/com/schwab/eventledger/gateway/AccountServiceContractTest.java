package com.schwab.eventledger.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.eventledger.common.AccountTransactionRequest;
import com.schwab.eventledger.common.EventType;
import com.schwab.eventledger.common.TraceHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class AccountServiceContractTest {
    @Test
    void gatewaySendsExpectedApplyTransactionContractToAccountService() {
        var client = new AccountClient(
                new RestTemplateBuilder(),
                new AccountServiceProperties("http://account-service", 100, 1, 0, 0, 1, 5000),
                new SimpleCircuitBreaker(
                        new AccountServiceProperties("http://account-service", 100, 1, 0, 0, 1, 5000),
                        Clock.systemUTC()
                ),
                new JsonLogger(new ObjectMapper().findAndRegisterModules())
        );
        var server = MockRestServiceServer.bindTo(client.restTemplate()).build();
        try {
            TraceContext.set("trace-contract-001");
            server.expect(once(), requestTo("http://account-service/accounts/acct-contract/transactions"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header(TraceHeaders.TRACE_ID, "trace-contract-001"))
                    .andExpect(jsonPath("$.eventId").value("contract-evt-001"))
                    .andExpect(jsonPath("$.type").value("CREDIT"))
                    .andExpect(jsonPath("$.amount").value(42.50))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON));

            client.applyTransaction("acct-contract", new AccountTransactionRequest(
                    "contract-evt-001",
                    EventType.CREDIT,
                    new BigDecimal("42.50"),
                    "USD",
                    Instant.parse("2026-05-15T14:02:11Z")
            ));

            server.verify();
        } finally {
            TraceContext.clear();
        }
    }
}
