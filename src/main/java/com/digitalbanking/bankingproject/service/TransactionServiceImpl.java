package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.constants.AccountStatus;
import com.digitalbanking.bankingproject.constants.ApplicationConstants;
import com.digitalbanking.bankingproject.constants.TransactionStatus;
import com.digitalbanking.bankingproject.dto.TransactionInRangeRequestDTO;
import com.digitalbanking.bankingproject.dto.TransactionRequestDTO;
import com.digitalbanking.bankingproject.dto.TransactionResponseDTO;
import com.digitalbanking.bankingproject.model.Account;
import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.model.Transaction;
import com.digitalbanking.bankingproject.model.TransactionLimit;
import com.digitalbanking.bankingproject.repository.AccountRepository;
import com.digitalbanking.bankingproject.repository.PersonRepository;
import com.digitalbanking.bankingproject.repository.TransactionLimitRepository;
import com.digitalbanking.bankingproject.repository.TransactionRepository;
import com.digitalbanking.bankingproject.service.declarations.TransactionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final TransactionLimitRepository transactionLimitRepository;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            PersonRepository personRepository,
            TransactionLimitRepository transactionLimitRepository){
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
        this.transactionLimitRepository = transactionLimitRepository;
    }

    @Transactional
    @Override
    public TransactionResponseDTO transfer(String email,TransactionRequestDTO transactionRequestDTO) {

        // ----------------------
        // Prevents deadlock
        // When a transaction from fromAcc is locked to do make a transfer to toAcc
        // If another transaction to toAcc wants to make a transfer to fromAcc, fromAcc will be locked
        // Here we force the transaction to have the same order based on accId
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

        // Not done, currency exchange
//        if (accountTo.getCurrency() != accountFrom.getCurrency()){
//            double rate = exchange(accountFrom.getCurrency(), accountTo.getCurrency());
//            amountToTransfer = amountToTransfer.multiply(BigDecimal.valueOf(rate));
//        }

        LocalTime timeNow = LocalTime.now();
        LocalDate dateNow = LocalDate.now();

        List<Transaction> transactionsToday = transactionRepository.findAllByFromAccountIdAndCreatedAtDate(accountFrom.getId(),dateNow);

        TransactionLimit transactionLimit = transactionLimitRepository.findByPersonId(person.getId())
                .orElseThrow(() -> new RuntimeException("User not found for id: " + person.getId()));

        BigDecimal totalAmountTransactionsToday = BigDecimal.ZERO;

        if (!transactionsToday.isEmpty()){
            for (Transaction t : transactionsToday){
                totalAmountTransactionsToday = totalAmountTransactionsToday.add(t.getAmount());
            }
            if (totalAmountTransactionsToday.compareTo(BigDecimal.valueOf(transactionLimit.getDailyLimit())) > 0){
                throw new RuntimeException("Daily limit amount of " + transactionLimit.getDailyLimit() + " for transactions reached: " + totalAmountTransactionsToday);
            }
        }

        if (transactionLimit.getPerTransactionLimit() < amountToTransfer.doubleValue() ){
            throw new RuntimeException("Per transaction limit is set to maximum: " + transactionLimit.getPerTransactionLimit());
        }

        if (!(transactionsToday.size() < transactionLimit.getMaxTransactionsLimitDaily())){
            throw new RuntimeException("Max transaction limit today reached");
        }

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
                TransactionStatus.COMPLETED,
                transactionRequestDTO.description(),
                dateNow,
                timeNow

        );

        accountRepository.save(accountFrom);
        accountRepository.save(accountTo);

        return toDTO(transactionRepository.save(transaction));
    }

    @Override
    public List<TransactionResponseDTO> transactionsInRangeForUser(String email,TransactionInRangeRequestDTO transactionInRangeRequestDTO){
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User doesnt' exist for email: " + email));
        Account account = accountRepository.findByPersonId(person.getId())
                .orElseThrow(() -> new RuntimeException("No account found for account: " + person.getId()));

        List<TransactionResponseDTO> transactions =
                transactionRepository.findAllByFromAccountIdAndCreatedAtDateBetween(
                        account.getId(),
                        transactionInRangeRequestDTO.dateFrom(),
                        transactionInRangeRequestDTO.dateTo())
                .stream()
                .map(transaction -> toDTO(transaction))
                .toList();

        return transactions;
    }

    private double exchange(String from, String to){
        Map<String, Map<String,Double>> rates = new HashMap<>();

        rates.put("RO", new HashMap<>());
        rates.get("RO").put("EU", 0.19);
        rates.get("RO").put("GB", 0.16);
        rates.get("RO").put("US", 0.22);

        rates.put("EU", new HashMap<>());
        rates.get("EU").put("GB", 0.87);
        rates.get("EU").put("US", 1.17);

        rates.put("GB", new HashMap<>());
        rates.get("GB").put("US", 1.35);

        double getRates = rates.get(from).get(to);

        return getRates;
    }
}
