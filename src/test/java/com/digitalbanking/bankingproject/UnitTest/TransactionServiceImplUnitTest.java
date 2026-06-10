package com.digitalbanking.bankingproject.UnitTest;

import com.digitalbanking.bankingproject.constants.AccountStatus;
import com.digitalbanking.bankingproject.constants.AccountType;
import com.digitalbanking.bankingproject.constants.PersonAccountStatus;
import com.digitalbanking.bankingproject.constants.TransactionStatus;
import com.digitalbanking.bankingproject.dto.TransactionRequestDTO;
import com.digitalbanking.bankingproject.dto.TransactionResponseDTO;
import com.digitalbanking.bankingproject.model.*;
import com.digitalbanking.bankingproject.repository.AccountRepository;
import com.digitalbanking.bankingproject.repository.PersonRepository;
import com.digitalbanking.bankingproject.repository.TransactionLimitRepository;
import com.digitalbanking.bankingproject.repository.TransactionRepository;
import com.digitalbanking.bankingproject.service.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplUnitTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private TransactionLimitRepository transactionLimitRepository;
    @InjectMocks
    private TransactionServiceImpl transactionService;

    private String emailJWT;
    private Person personFrom;
    private Person personTo;
    private Authority authorityFrom;
    private Authority authorityTo;
    private TransactionLimit transactionLimit;
    private Transaction transaction;
    private TransactionRequestDTO transactionRequestDTO;
    private Account accountFrom;
    private Account accountTo;
    private List<Transaction> transactionsToday;

    @BeforeEach
    void setup() {
        emailJWT = "raul@gmail.com";
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        transactionRequestDTO = new TransactionRequestDTO(
                1L,
                2L,
                new BigDecimal(20),
                "Something"
        );

        personFrom = new Person(
                1L,
                "raul@gmail.com",
                "Parola1234@asd",
                "Raul",
                "Mrn",
                123456789L,
                null,
                PersonAccountStatus.ONHOLD,
                new Date()
        );
        personTo = new Person(
                2L,
                "alex@gmail.com",
                "Parola1234@asd",
                "Alex",
                "Nasa",
                223456789L,
                null,
                PersonAccountStatus.ONHOLD,
                new Date()
        );

        authorityFrom = new Authority(1L, "CUSTOMER", personFrom);
        personFrom.setAuthorities(Set.of(authorityFrom));
        authorityTo = new Authority(2L, "CUSTOMER", personTo);
        personTo.setAuthorities(Set.of(authorityTo));

        accountFrom = new Account(
                1L,
                personFrom,
                "RO",
                "WHATEVER",
                BigDecimal.valueOf(30),
                AccountType.CURRENT,
                AccountStatus.ONHOLD,
                new Date()
        );
        accountTo = new Account(
                2L,
                personTo,
                "RO",
                "WHATEVER_TWO",
                BigDecimal.ZERO,
                AccountType.CURRENT,
                AccountStatus.ONHOLD,
                new Date()
        );

        transaction = new Transaction(
                1L,
                accountFrom.getId(),
                accountTo.getId(),
                transactionRequestDTO.amount(),
                accountFrom.getCurrency(),
                TransactionStatus.COMPLETED,
                transactionRequestDTO.description(),
                date,
                time
        );

        transactionsToday = new ArrayList<>(List.of(
                new Transaction(1L,
                        accountFrom.getId(),
                        accountTo.getId(),
                        transactionRequestDTO.amount(),
                        accountFrom.getCurrency(),
                        TransactionStatus.COMPLETED,
                        transactionRequestDTO.description(),
                        date,
                        time),
                new Transaction(2L,
                        accountFrom.getId(),
                        accountTo.getId(),
                        transactionRequestDTO.amount(),
                        accountFrom.getCurrency(),
                        TransactionStatus.COMPLETED,
                        transactionRequestDTO.description(),
                        date,
                        time)
        ));

        transactionLimit = new TransactionLimit(
                1L,
                personFrom,
                10000.0,
                5000.0,
                5
        );
    }

    // --------------------------------------------
    // SUCCESS
    // --------------------------------------------

    @Test
    void transfer_ShouldTransfer() {
        Long fromId = transactionRequestDTO.fromAccountId();
        Long toId   = transactionRequestDTO.toAccountId();

        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));
        when(transactionRepository.findAllByFromAccountIdAndCreatedAtDate(accountFrom.getId(), LocalDate.now()))
                .thenReturn(transactionsToday);
        when(transactionLimitRepository.findByPersonId(personFrom.getId()))
                .thenReturn(Optional.of(transactionLimit));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionResponseDTO response = transactionService.transfer(emailJWT, transactionRequestDTO);

        assertEquals(1L, response.fromAccountId());
        assertEquals(2L, response.toAccountId());
        assertEquals(0, BigDecimal.valueOf(20).compareTo(response.amount()));
        verify(transactionRepository).save(any(Transaction.class));
    }

    // --------------------------------------------
    // SAME ACCOUNT
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenFromAndToAccountAreSame() {
        TransactionRequestDTO sameAccountRequest = new TransactionRequestDTO(
                1L,
                1L,
                new BigDecimal(20),
                "Something"
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, sameAccountRequest));

        assertEquals("Cannot transfer to the same account", ex.getMessage());
    }

    // --------------------------------------------
    // FIRST ACCOUNT NOT FOUND
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenFirstAccountNotFound() {
        Long fromId = transactionRequestDTO.fromAccountId();
        Long toId   = transactionRequestDTO.toAccountId();
        Long firstId = fromId < toId ? fromId : toId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("First account not found"));
    }

    // --------------------------------------------
    // SECOND ACCOUNT NOT FOUND
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenSecondAccountNotFound() {
        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("Second account not found"));
    }

    // --------------------------------------------
    // PERSON NOT FOUND
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenPersonNotFound() {
        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("User doesn't exist for email"));
    }

    // --------------------------------------------
    // ACCOUNT OWNERSHIP
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenFromAccountDoesNotBelongToConnectedPerson() {
        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        // accountFrom belongs to personFrom, but the logged-in user is personTo
        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personTo)); // wrong person

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("Invalid account"));
        assertTrue(ex.getMessage().contains("choose your accounts"));
    }

    // --------------------------------------------
    // ACCOUNT STATUS - FROM
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenFromAccountIsDisabled() {
        accountFrom.setStatus(AccountStatus.DISABLED);

        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("Account is: DISABLED"));
    }

    @Test
    void transfer_ShouldThrow_WhenFromAccountIsExpired() {
        accountFrom.setStatus(AccountStatus.EXPIRED);

        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("Account is: EXPIRED"));
    }

    // --------------------------------------------
    // ACCOUNT STATUS - TO
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenToAccountIsDisabled() {
        accountTo.setStatus(AccountStatus.DISABLED);

        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("Account to transfer is: DISABLED"));
    }

    @Test
    void transfer_ShouldThrow_WhenToAccountIsExpired() {
        accountTo.setStatus(AccountStatus.EXPIRED);

        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("Account to transfer is: EXPIRED"));
    }

    // --------------------------------------------
    // TRANSACTION LIMIT - NOT FOUND
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenTransactionLimitNotFound() {
        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));
        when(transactionRepository.findAllByFromAccountIdAndCreatedAtDate(accountFrom.getId(), LocalDate.now()))
                .thenReturn(new ArrayList<>());
        when(transactionLimitRepository.findByPersonId(personFrom.getId())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("User not found for id"));
    }

    // --------------------------------------------
    // DAILY LIMIT EXCEEDED
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenDailyLimitExceeded() {
        // Each transaction in transactionsToday is 20, two of them = 40, limit is 30
        transactionLimit = new TransactionLimit(
                1L, personFrom,
                30.0,
                5000.0,
                5);

        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));
        when(transactionRepository.findAllByFromAccountIdAndCreatedAtDate(accountFrom.getId(), LocalDate.now()))
                .thenReturn(transactionsToday); // total = 40 > 30
        when(transactionLimitRepository.findByPersonId(personFrom.getId()))
                .thenReturn(Optional.of(transactionLimit));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("Daily limit amount of"));
        assertTrue(ex.getMessage().contains("reached"));
    }

    // --------------------------------------------
    // PER-TRANSACTION LIMIT EXCEEDED
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenPerTransactionLimitExceeded() {
        // Amount to transfer is 20, per-transaction limit is 10
        transactionLimit = new TransactionLimit(
                1L,
                personFrom,
                50.0,
                10.0,
                5);

        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));
        when(transactionRepository.findAllByFromAccountIdAndCreatedAtDate(accountFrom.getId(), LocalDate.now()))
                .thenReturn(new ArrayList<>());
        when(transactionLimitRepository.findByPersonId(personFrom.getId()))
                .thenReturn(Optional.of(transactionLimit));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertTrue(ex.getMessage().contains("Per transaction limit is set to maximum"));
    }

    // --------------------------------------------
    // MAX DAILY TRANSACTION COUNT EXCEEDED
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenMaxDailyTransactionCountReached() {
        // transactionsToday has 2 entries, maxTransactionsLimitDaily is 2 -> not (2 < 2) -> throws
        transactionLimit = new TransactionLimit(
                1L, personFrom,
                10000.0,
                5000.0,
                2);

        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));
        when(transactionRepository.findAllByFromAccountIdAndCreatedAtDate(accountFrom.getId(), LocalDate.now()))
                .thenReturn(transactionsToday);
        when(transactionLimitRepository.findByPersonId(personFrom.getId()))
                .thenReturn(Optional.of(transactionLimit));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertEquals("Max transaction limit today reached", ex.getMessage());
    }

    // --------------------------------------------
    // INSUFFICIENT FUNDS
    // --------------------------------------------

    @Test
    void transfer_ShouldThrow_WhenInsufficientFunds() {
        // Balance is 30, amount is 20, plus BANK_TRANSFER_FEE - if fee > 10, this fails
        accountFrom.setBalance(BigDecimal.valueOf(1));

        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));
        when(transactionRepository.findAllByFromAccountIdAndCreatedAtDate(accountFrom.getId(), LocalDate.now()))
                .thenReturn(new ArrayList<>());
        when(transactionLimitRepository.findByPersonId(personFrom.getId()))
                .thenReturn(Optional.of(transactionLimit));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.transfer(emailJWT, transactionRequestDTO));

        assertEquals("Insufficient funds", ex.getMessage());
    }

    // --------------------------------------------
    // BALANCES UPDATED CORRECTLY
    // --------------------------------------------

    @Test
    void transfer_ShouldUpdateBalancesCorrectly() {
        // Balance 30, transfer 20, fee comes from ApplicationConstants.BANK_TRANSFER_FEE
        // After transfer: accountFrom.balance = 30 - 20 - fee, accountTo.balance = 0 + 20
        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));
        when(transactionRepository.findAllByFromAccountIdAndCreatedAtDate(accountFrom.getId(), LocalDate.now()))
                .thenReturn(new ArrayList<>());
        when(transactionLimitRepository.findByPersonId(personFrom.getId()))
                .thenReturn(Optional.of(transactionLimit));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        transactionService.transfer(emailJWT, transactionRequestDTO);

        BigDecimal expectedToBalance = BigDecimal.valueOf(20);
        assertEquals(0, accountTo.getBalance().compareTo(expectedToBalance)); // 0 means equals

        // accountFrom should have less than the original 30 (deducted amount + fee)1
        assertTrue(accountFrom.getBalance().compareTo(BigDecimal.valueOf(30)) < 0);

        verify(accountRepository, times(2)).save(any(Account.class));
    }

    // --------------------------------------------
    // NO TRANSACTIONS TODAY - LIMITS NOT TRIGGERED
    // --------------------------------------------

    @Test
    void transfer_ShouldTransfer_WhenNoTransactionsTodayExist() {
        Long fromId   = transactionRequestDTO.fromAccountId();
        Long toId     = transactionRequestDTO.toAccountId();
        Long firstId  = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId   : fromId;

        when(accountRepository.findByIdForTransaction(firstId)).thenReturn(Optional.of(accountFrom));
        when(accountRepository.findByIdForTransaction(secondId)).thenReturn(Optional.of(accountTo));
        when(personRepository.findByEmail(emailJWT)).thenReturn(Optional.of(personFrom));
        when(transactionRepository.findAllByFromAccountIdAndCreatedAtDate(accountFrom.getId(), LocalDate.now()))
                .thenReturn(new ArrayList<>()); // empty - no transactions today
        when(transactionLimitRepository.findByPersonId(personFrom.getId()))
                .thenReturn(Optional.of(transactionLimit));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionResponseDTO response = transactionService.transfer(emailJWT, transactionRequestDTO);

        assertNotNull(response);
        verify(transactionRepository).save(any(Transaction.class));
    }
}