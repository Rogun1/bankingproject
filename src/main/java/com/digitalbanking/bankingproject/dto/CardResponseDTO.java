package com.digitalbanking.bankingproject.dto;

import com.digitalbanking.bankingproject.constants.CardStatus;

import java.time.LocalDate;

public record CardResponseDTO(
        String cardNumber,
        Integer cvv,
        LocalDate expirationDate,
        CardStatus cardStatus
) {
}
