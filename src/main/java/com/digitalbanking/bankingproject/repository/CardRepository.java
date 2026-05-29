package com.digitalbanking.bankingproject.repository;

import com.digitalbanking.bankingproject.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface CardRepository extends JpaRepository<Card,Long> {

    Boolean existsCardByAccountId(Long personId);
    Boolean existsByCardNumber(Long cardNumber);
    Boolean existsByCvv(Integer cvv);
    Optional<Card> findById(Long cardId);
    Optional<Card> findByAccountId(Long accountId);
}