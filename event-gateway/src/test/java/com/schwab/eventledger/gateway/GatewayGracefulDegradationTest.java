package com.schwab.eventledger.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.eventledger.common.AccountDetailsResponse;
import com.schwab.eventledger.common.AccountTransactionRequest;
import com.schwab.eventledger.common.BalanceResponse;
import com.schwab.eventledger.common.EventType;
import com.schwab.eventledger.common.TransactionEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayGracefulDegradationTest {
    private EventRepository eventRepository;
    private EventService eventService;
    private ApiExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:gateway-graceful-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        var jdbcTemplate = new JdbcTemplate(dataSource);
        var objectMapper = new ObjectMapper().findAndRegisterModules();
        new GatewaySchema(jdbcTemplate);

        eventRepository = new EventRepository(jdbcTemplate, objectMapper);
        eventService = new EventService(eventRepository, new UnavailableAccountClient(objectMapper), new EventSubmissionLocks());
        exceptionHandler = new ApiExceptionHandler();
    }

    @Test
    void postEventsFailsGracefullyAndDoesNotLeavePartialGatewayStateWhenAccountServiceIsDown() {
        var request = event("graceful-post-001", "acct-graceful-down");

        assertThatThrownBy(() -> eventService.submit(request))
                .isInstanceOf(AccountUnavailableException.class)
                .hasMessage("Account Service is unavailable for operation apply_transaction");

        assertThat(eventRepository.findById("graceful-post-001")).isEmpty();

        var error = exceptionHandler.accountUnavailable(
                new AccountUnavailableException("Account Service is unavailable for operation apply_transaction"));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(error.getBody()).isNotNull();
        assertThat(error.getBody().error()).isEqualTo("Account Service Unavailable");
        assertThat(error.getBody().messages()).containsExactly("Account Service is unavailable for operation apply_transaction");
    }

    @Test
    void gatewayLocalEventReadsStillWorkWhenAccountServiceIsDown() {
        eventRepository.save(event("graceful-existing-001", "acct-graceful-local"));

        var singleEvent = eventService.event("graceful-existing-001");
        var accountEvents = eventService.eventsForAccount("acct-graceful-local");

        assertThat(singleEvent.eventId()).isEqualTo("graceful-existing-001");
        assertThat(accountEvents).hasSize(1);
        assertThat(accountEvents.get(0).eventId()).isEqualTo("graceful-existing-001");
    }

    @Test
    void balanceQueriesReturnClearUnavailableErrorWhenAccountServiceIsDown() {
        var error = exceptionHandler.accountUnavailable(
                new AccountUnavailableException("Account Service circuit is open; try again shortly"));

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(error.getBody()).isNotNull();
        assertThat(error.getBody().error()).isEqualTo("Account Service Unavailable");
        assertThat(error.getBody().messages()).containsExactly("Account Service circuit is open; try again shortly");
    }

    private TransactionEventRequest event(String eventId, String accountId) {
        return new TransactionEventRequest(
                eventId,
                accountId,
                EventType.CREDIT,
                new BigDecimal("10.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of()
        );
    }

    private static final class UnavailableAccountClient extends AccountClient {
        private UnavailableAccountClient(ObjectMapper objectMapper) {
            super(
                    new RestTemplateBuilder(),
                    new AccountServiceProperties("http://localhost:1", 100, 1, 0, 1, 5000),
                    new SimpleCircuitBreaker(
                            new AccountServiceProperties("http://localhost:1", 100, 1, 0, 1, 5000),
                            Clock.systemUTC()
                    ),
                    new JsonLogger(objectMapper)
            );
        }

        @Override
        void applyTransaction(String accountId, AccountTransactionRequest request) {
            throw new AccountUnavailableException("Account Service is unavailable for operation apply_transaction");
        }

        @Override
        BalanceResponse balance(String accountId) {
            throw new AccountUnavailableException("Account Service circuit is open; try again shortly");
        }

        @Override
        AccountDetailsResponse account(String accountId) {
            throw new AccountUnavailableException("Account Service circuit is open; try again shortly");
        }
    }
}
