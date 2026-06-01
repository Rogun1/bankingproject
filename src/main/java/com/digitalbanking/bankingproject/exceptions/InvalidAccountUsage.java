package com.digitalbanking.bankingproject.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidAccountUsage extends RuntimeException {
    public InvalidAccountUsage(String message) {
        super(message);
    }
}
