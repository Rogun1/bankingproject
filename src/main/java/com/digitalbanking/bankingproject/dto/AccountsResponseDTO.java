package com.digitalbanking.bankingproject.dto;

import com.digitalbanking.bankingproject.constants.AccountStatus;
import com.digitalbanking.bankingproject.constants.AccountType;

public record AccountsResponseDTO(
        String currency,
        String iban,
        Double balance,
        AccountType accountType,
        AccountStatus accountStatus
) {
}
