package com.schwab.eventledger.gateway;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes Gateway-owned embedded database tables.
 *
 * <p>Keeping schema creation local to the Gateway reinforces service ownership:
 * event ledger rows and pending outbox rows are not shared with the Account
 * Service database.</p>
 */
@Component
class GatewaySchema {
    GatewaySchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create table if not exists events (
                    event_id varchar(128) primary key,
                    account_id varchar(128) not null,
                    type varchar(16) not null,
                    amount decimal(19, 4) not null,
                    currency varchar(3) not null,
                    event_timestamp timestamp with time zone not null,
                    metadata_json clob,
                    received_at timestamp with time zone not null
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_events_account_ts on events(account_id, event_timestamp, event_id)");
        jdbcTemplate.execute("""
                create table if not exists pending_account_events (
                    event_id varchar(128) primary key,
                    attempt_count integer not null,
                    next_attempt_at timestamp with time zone not null,
                    last_error varchar(512),
                    created_at timestamp with time zone not null,
                    updated_at timestamp with time zone not null,
                    constraint fk_pending_event foreign key (event_id) references events(event_id) on delete cascade
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_pending_account_events_next_attempt on pending_account_events(next_attempt_at)");
    }
}
