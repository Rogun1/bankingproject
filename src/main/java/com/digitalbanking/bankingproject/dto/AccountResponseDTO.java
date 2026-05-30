package com.digitalbanking.bankingproject.dto;

import com.digitalbanking.bankingproject.constants.AccountStatus;
import com.digitalbanking.bankingproject.constants.AccountType;

import java.math.BigDecimal;

public record AccountResponseDTO(
        String currency,
        String iban,
        BigDecimal balance,
        AccountType accountType,
        AccountStatus accountStatus
) {
}
