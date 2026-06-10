package com.digitalbanking.bankingproject.dto;

import jakarta.validation.constraints.NotNull;

public record CardStatusBlockRequestDTO(
        @NotNull String cardStatus
) {
}
