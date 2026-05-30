package com.digitalbanking.bankingproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record TransactionInRangeRequestDTO(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateFrom,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateTo
) {

}
