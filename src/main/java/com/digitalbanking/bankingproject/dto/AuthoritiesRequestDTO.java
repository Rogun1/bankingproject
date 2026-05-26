package com.digitalbanking.bankingproject.dto;

import com.digitalbanking.bankingproject.constants.Authorities;

import java.util.Set;

public record AuthoritiesRequestDTO(Set<Authorities> authorities) {
}
