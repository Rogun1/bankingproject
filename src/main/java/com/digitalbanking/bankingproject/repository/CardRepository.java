package com.digitalbanking.bankingproject.repository;

import com.digitalbanking.bankingproject.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CardRepository extends JpaRepository<Card,Long> {

    Boolean existsCardByAccountId(Long customerId);
    Boolean existsByCardNumber(Long cardNumber);
    Boolean existsByCvv(Integer cvv);
}