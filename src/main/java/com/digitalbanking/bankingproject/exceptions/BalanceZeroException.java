package com.digitalbanking.bankingproject.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class BalanceZeroException extends RuntimeException {
    public BalanceZeroException(String message) {
        super(message);
    }
}
