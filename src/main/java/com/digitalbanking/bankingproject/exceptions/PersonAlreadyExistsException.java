package com.digitalbanking.bankingproject.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PersonAlreadyExistsException extends RuntimeException {
    public PersonAlreadyExistsException(String message) {
        super(message);
    }
}
