package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.constants.AccountStatus;
import com.digitalbanking.bankingproject.constants.ApplicationConstants;
import com.digitalbanking.bankingproject.constants.TransactionStatus;
import com.digitalbanking.bankingproject.dto.TransactionRequestDTO;
import com.digitalbanking.bankingproject.dto.TransactionResponseDTO;
import com.digitalbanking.bankingproject.model.Account;
import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.model.Transaction;
import com.digitalbanking.bankingproject.repository.AccountRepository;
import com.digitalbanking.bankingproject.repository.PersonRepository;
import com.digitalbanking.bankingproject.repository.TransactionRepository;
import com.digitalbanking.bankingproject.service.declarations.TransactionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            PersonRepository personRepository){
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
    }

    @Transactional
    @Override
    public TransactionResponseDTO transfer(String email,TransactionRequestDTO transactionRequestDTO) {

        // ----------------------
        // Prevents deadlock
        // When fromAcc is locked to do make a transfer to toAcc
        // If toAcc wants to make a transfer to fromAcc, fromAcc will be locked
        Long fromId = transactionRequestDTO.fromAccountId();
        Long toId = transactionRequestDTO.toAccountId();

        if (fromId.equals(toId)){
            throw new RuntimeException("Cannot transfer to the same account");
        }

        Long firstId = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId : fromId;

        Account first = accountRepository.findByIdForTransaction(firstId).orElseThrow(
                () -> new RuntimeException("First account not found for id: " +  firstId)
        );
        Account second = accountRepository.findByIdForTransaction(secondId).orElseThrow(
                () -> new RuntimeException("Second account not found for id: " +  secondId)
        );

        //Is the first account the one that transfers ?
        Account accountFrom = (first.getId().equals(fromId)) ? first : second;
        Account accountTo = (first.getId().equals(toId)) ? first : second;

        // ----------------------
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User doesnt' exist for email: " + email));

        if (!accountFrom.getPerson().getId().equals(person.getId())){
            throw new RuntimeException("Invalid account " + fromId + ", choose your accounts");
        }

        if (accountFrom.getStatus() == AccountStatus.DISABLED
        || accountFrom.getStatus() == AccountStatus.EXPIRED){
            throw new RuntimeException("Account is: " + accountFrom.getStatus());
        }
        if (accountTo.getStatus() == AccountStatus.DISABLED
                || accountTo.getStatus() == AccountStatus.EXPIRED){
            throw new RuntimeException("Account to transfer is: " + accountTo.getStatus());
        }

        BigDecimal bankTransferFee = ApplicationConstants.BANK_TRANSFER_FEE;
        BigDecimal amountToTransfer = transactionRequestDTO.amount();
        BigDecimal currentAccountBalance = accountFrom.getBalance();

        if (currentAccountBalance.compareTo(amountToTransfer.add(bankTransferFee)) < 0){
            throw new RuntimeException("Insufficient funds");
        }

        accountFrom.setBalance(currentAccountBalance
                .subtract(amountToTransfer)
                .subtract(bankTransferFee));

        accountTo.setBalance(accountTo.getBalance()
                .add(amountToTransfer));

        Transaction transaction = new Transaction(
                null,
                accountFrom.getId(),
                accountTo.getId(),
                amountToTransfer,
                accountFrom.getCurrency(),
                TransactionStatus.ONHOLD,
                transactionRequestDTO.description(),
                new Date()
        );

        accountRepository.save(accountFrom);
        accountRepository.save(accountTo);

        return toDTO(transactionRepository.save(transaction));
    }
}
