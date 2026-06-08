package com.digitalbanking.bankingproject.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PersonAlreadyHasRoleException extends RuntimeException {
    public PersonAlreadyHasRoleException(String message) {
        super(message);
    }
}
