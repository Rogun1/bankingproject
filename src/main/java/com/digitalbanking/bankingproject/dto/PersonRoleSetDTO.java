package com.digitalbanking.bankingproject.dto;

import jakarta.validation.constraints.NotNull;

public record PersonRoleSetDTO(
        @NotNull String role
) {
}
