package com.digitalbanking.bankingproject.service.declarations;

import com.digitalbanking.bankingproject.dto.TransactionInRangeRequestDTO;
import com.digitalbanking.bankingproject.dto.TransactionRequestDTO;
import com.digitalbanking.bankingproject.dto.TransactionResponseDTO;
import com.digitalbanking.bankingproject.model.Transaction;

import java.util.List;

public interface TransactionService {

    TransactionResponseDTO transfer(String email,TransactionRequestDTO transactionRequestDTO);
    List<TransactionResponseDTO> transactionsInRangeForUser(String email,TransactionInRangeRequestDTO transactionInRangeRequestDTO);

    default TransactionResponseDTO toDTO(Transaction transaction){
        return new TransactionResponseDTO(
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAtDate(),
                transaction.getCreatedAtTime()
        );
    }
}
