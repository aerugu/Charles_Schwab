package com.schwab.eventledger.gateway;

class AccountUnavailableException extends RuntimeException {
    AccountUnavailableException(String message) {
        super(message);
    }

    AccountUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
