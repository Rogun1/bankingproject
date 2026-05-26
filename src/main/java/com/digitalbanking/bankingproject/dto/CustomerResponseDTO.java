package com.digitalbanking.bankingproject.dto;

import java.util.List;

public record CustomerResponseDTO(
        String userName,
        String email,
        List<String> authorities,
        String status) {
}
