package com.digitalbanking.bankingproject.dto;


import com.digitalbanking.bankingproject.constants.TransactionStatus;

import java.math.BigDecimal;
import java.util.Date;

public record TransactionResponseDTO(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        TransactionStatus status,
        String description,
        Date date
) {
}
