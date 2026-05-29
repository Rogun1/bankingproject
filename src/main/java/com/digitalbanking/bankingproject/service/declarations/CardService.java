package com.digitalbanking.bankingproject.service.declarations;

import com.digitalbanking.bankingproject.constants.CardStatus;
import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.CardResponseDTO;
import com.digitalbanking.bankingproject.dto.CardStatusBlockRequestDTO;
import com.digitalbanking.bankingproject.model.Card;

public interface CardService {

    CardResponseDTO getCard(String email, AccountRequestDTO accountRequestDTO) throws Exception;
    CardResponseDTO blockCard(String email, Long cardId, CardStatusBlockRequestDTO cardStatus);

    default CardResponseDTO toDTO(Card card){
        return new CardResponseDTO(
                card.getCardNumber(),
                card.getCvv(),
                card.getExpirationDate(),
                card.getStatus()
        );
    }
}
