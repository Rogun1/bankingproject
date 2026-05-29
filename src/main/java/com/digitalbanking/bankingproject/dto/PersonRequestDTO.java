package com.digitalbanking.bankingproject.dto;

public record PersonRequestDTO(
        String firstName,
        String lastName,
        String email,
        Long CNP,
        String pwd) {
}
