package com.digitalbanking.bankingproject.dto;

public record CustomerRequestDTO(
        String firstName,
        String lastName,
        String email,
        Long CNP,
        String pwd) {
}
