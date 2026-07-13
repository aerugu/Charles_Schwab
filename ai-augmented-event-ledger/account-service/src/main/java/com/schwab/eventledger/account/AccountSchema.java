package com.schwab.eventledger.account;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes Account Service-owned embedded database tables.
 */
@Component
class AccountSchema {
    AccountSchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create table if not exists transactions (
                    event_id varchar(128) primary key,
                    account_id varchar(128) not null,
                    type varchar(16) not null,
                    amount decimal(19, 4) not null,
                    currency varchar(3) not null,
                    event_timestamp timestamp with time zone not null,
                    applied_at timestamp with time zone not null
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_transactions_account_ts on transactions(account_id, event_timestamp)");
    }
}
