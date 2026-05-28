package com.digitalbanking.bankingproject.controller;

import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.CardResponseDTO;
import com.digitalbanking.bankingproject.service.declarations.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public CardResponseDTO getCard(Authentication authentication,@RequestBody AccountRequestDTO accountRequestDTO){
        return cardService.getCard(authentication.getName(), accountRequestDTO);
    }
}
