package com.digitalbanking.bankingproject.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AccountDisabledOrExpiredException extends RuntimeException {
    public AccountDisabledOrExpiredException(String message) {
        super(message);
    }
}
