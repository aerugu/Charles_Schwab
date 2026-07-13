package com.schwab.eventledger.gateway;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
    }
}
