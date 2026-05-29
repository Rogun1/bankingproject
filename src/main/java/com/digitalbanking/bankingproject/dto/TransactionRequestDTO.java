package com.digitalbanking.bankingproject.dto;

import java.math.BigDecimal;

public record TransactionRequestDTO(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String description
) {
}
