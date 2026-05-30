package com.digitalbanking.bankingproject.controller;

import com.digitalbanking.bankingproject.dto.TransactionInRangeRequestDTO;
import com.digitalbanking.bankingproject.dto.TransactionRequestDTO;
import com.digitalbanking.bankingproject.dto.TransactionResponseDTO;
import com.digitalbanking.bankingproject.service.declarations.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public TransactionResponseDTO transfer(Authentication authentication, @RequestBody TransactionRequestDTO transactionRequestDTO){
        return transactionService.transfer(authentication.getName(),transactionRequestDTO);
    }

    @GetMapping
    public List<TransactionResponseDTO> transactionsInRangeForUser(Authentication authentication,@RequestBody TransactionInRangeRequestDTO transactionInRangeRequestDTO){
        return transactionService.transactionsInRangeForUser(authentication.getName(), transactionInRangeRequestDTO);
    }
    // Transactions between different currency logic TO DO
    // See all transactions by date range for employers TO DO
    // Transaction limitation extension by employer TO DO
}
