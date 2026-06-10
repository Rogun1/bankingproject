package com.digitalbanking.bankingproject.controller;

import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.CardResponseDTO;
import com.digitalbanking.bankingproject.dto.CardStatusBlockRequestDTO;
import com.digitalbanking.bankingproject.service.declarations.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public CardResponseDTO getCard(Authentication authentication,@RequestBody @Valid AccountRequestDTO accountRequestDTO){
        return cardService.getCard(authentication.getName(), accountRequestDTO);
    }

    @PatchMapping("/{cardId}/block")
    public CardResponseDTO blockCard(Authentication authentication, @PathVariable @NotNull Long cardId, @RequestBody @Valid CardStatusBlockRequestDTO cardStatus){
        return cardService.blockCard(authentication.getName(), cardId, cardStatus);
    }
}
