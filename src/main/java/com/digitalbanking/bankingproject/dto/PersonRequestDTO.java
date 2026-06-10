package com.digitalbanking.bankingproject.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record PersonRequestDTO(
        @NotNull String firstName,
        @NotNull String lastName,
        @Email String email,
        @NotNull Long CNP,
        @NotNull String pwd) {
}
