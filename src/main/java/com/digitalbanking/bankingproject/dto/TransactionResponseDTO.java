package com.digitalbanking.bankingproject.dto;


import com.digitalbanking.bankingproject.constants.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TransactionResponseDTO(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        TransactionStatus status,
        String description,
        LocalDate date,
        LocalTime time
) {
}
