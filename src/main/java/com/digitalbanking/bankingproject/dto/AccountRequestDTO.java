package com.digitalbanking.bankingproject.dto;

import jakarta.validation.constraints.NotNull;

public record AccountRequestDTO(
        @NotNull String currency
) {
}
