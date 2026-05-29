package com.digitalbanking.bankingproject.dto;

import java.util.List;

public record PersonResponseDTO(
        String userName,
        String email,
        List<String> authorities,
        String status) {
}
