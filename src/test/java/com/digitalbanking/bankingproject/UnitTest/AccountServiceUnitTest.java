package com.digitalbanking.bankingproject.UnitTest;

import com.digitalbanking.bankingproject.constants.*;
import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.AccountResponseDTO;
import com.digitalbanking.bankingproject.dto.AccountsResponseDTO;
import com.digitalbanking.bankingproject.dto.PersonRequestDTO;
import com.digitalbanking.bankingproject.exceptions.AlreadyExistsException;
import com.digitalbanking.bankingproject.exceptions.BalanceZeroException;
import com.digitalbanking.bankingproject.exceptions.NotFoundException;
import com.digitalbanking.bankingproject.model.*;
import com.digitalbanking.bankingproject.repository.*;
import com.digitalbanking.bankingproject.service.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceUnitTest {
    @Mock
    private PersonRepository personRepository;
    @Mock
    private AuthorityRepository authorityRepository;
    @Mock
    private TransactionLimitRepository transactionLimitRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CardRepository cardRepository;
    @InjectMocks
    private AccountServiceImpl accountService;

    private Person person;
    private PersonRequestDTO personRequestDTO;
    private String hashedPwd;
    private TransactionLimit transactionLimit;
    private AccountRequestDTO accountRequestDTO;
    private Account account;
    private String emailJWT;

    @BeforeEach
    void setup() {
        emailJWT = "raul@gmail.com";
        personRequestDTO = new PersonRequestDTO(
                "Raul",
                "Mrn",
                "raul@gmail.com",
                123456789L,
                "Parolae@123"
        );

        hashedPwd = passwordEncoder.encode(personRequestDTO.pwd());

        person = new Person(
                null,
                personRequestDTO.email(),
                hashedPwd,
                personRequestDTO.firstName(),
                personRequestDTO.lastName(),
                personRequestDTO.CNP(),
                null,
                PersonAccountStatus.ACTIVE,
                new Date()
        );

        Set<Authority> authorities = new HashSet<>();
        authorities.add(
                new Authority(
                        null,
                        "ROLE_" + PersonRole.CUSTOMER,
                        person));
        person.setAuthorities(authorities);

        transactionLimit = new TransactionLimit(
                null,
                person,
                2000.0,
                500.0,
                5
        );

        accountRequestDTO = new AccountRequestDTO(
                "RO"
        );

        account = new Account(
                null,
                person,
                accountRequestDTO.currency(),
                "WHATEVERIBAN",
                BigDecimal.valueOf(20.0),
                AccountType.CURRENT,
                AccountStatus.ACTIVE,
                new Date()
        );

    }

    // --------------------------------------------
    // SUCCESS
    // --------------------------------------------

    @Test
    void register_ShouldSaveAccount(){
        when(personRepository.existsByEmail(emailJWT))
                .thenReturn(true);
        when(personRepository.findByEmail(person.getEmail()))
                .thenReturn(Optional.of(person));
        when(accountRepository.existsByCurrencyAndPersonId(accountRequestDTO.currency(), person.getId()))
                .thenReturn(false);
        when(accountRepository.save(any(Account.class)))
                .thenReturn(account);

        AccountResponseDTO accountResponseDTO = accountService.register(emailJWT,accountRequestDTO);

        assertEquals("RO", accountResponseDTO.currency());
        assertEquals(BigDecimal.valueOf(20.0), accountResponseDTO.balance());
        assertEquals("ACTIVE", accountResponseDTO.accountStatus().toString());
        verify(accountRepository).save(any(Account.class));
    }

    // --------------------------------------------
    // FAILED - When person doesn't exists by email
    // --------------------------------------------

    @Test
    void register_ShouldFail_WhenDoestExistsByEmail(){
        when(personRepository.existsByEmail(emailJWT))
                .thenReturn(false);
        when(personRepository.findByEmail(person.getEmail()))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> accountService.register(emailJWT,accountRequestDTO));

        assertTrue(ex.getMessage().contains("User doesn't exist by email"));
        //assertEquals("User doesn't exist by email", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    // --------------------------------------------
    // FAILED - When exists by Email
    // --------------------------------------------

    @Test
    void register_ShouldFail_WhenExistsByEmail(){
        when(personRepository.existsByEmail(emailJWT))
                .thenReturn(true);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> accountService.register(emailJWT,accountRequestDTO));

        assertTrue(ex.getMessage().contains("User doesn't exist by email"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    // --------------------------------------------
    // FAILED - When exists by Currency and Person Id
    // --------------------------------------------

    @Test
    void register_ShouldFail_WhenExistsByCurrencyAndPersonId(){
        when(personRepository.existsByEmail(emailJWT))
                .thenReturn(true);
        when(personRepository.findByEmail(person.getEmail()))
                .thenReturn(Optional.of(person));
        when(accountRepository.existsByCurrencyAndPersonId(accountRequestDTO.currency(), person.getId()))
                .thenReturn(true);

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> accountService.register(emailJWT,accountRequestDTO));

        assertTrue(ex.getMessage().contains("already exists with currency"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    // --------------------------------------------
    // SUCCESS
    // --------------------------------------------

    @Test
    void accounts_ShouldSuccess_WhenAllAccountsFoundForPersonId(){
        when(personRepository.findByEmail(person.getEmail()))
                .thenReturn(Optional.of(person));
        when(accountRepository.findAllByPersonId(person.getId()))
                .thenReturn(List.of(account));

        List<AccountsResponseDTO> accountsResponseDTO = accountService.accounts(person.getEmail());

        assertEquals(1, accountsResponseDTO.size());
    }

    // --------------------------------------------
    // FAILED - When person not found by email
    // --------------------------------------------

    @Test
    void accounts_ShouldFail_WhenPersonNotFoundByEmail(){
        when(personRepository.findByEmail(person.getEmail()))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> accountService.accounts(person.getEmail()));

        assertTrue(ex.getMessage().contains("User doesn't exist by id"));
    }

    // --------------------------------------------
    // FAILED - When no account found for person id
    // --------------------------------------------

    @Test
    void accounts_ShouldFail_WhenNoAccountFoundForPersonId(){
        when(personRepository.findByEmail(person.getEmail()))
                .thenReturn(Optional.of(person));
        when(accountRepository.findAllByPersonId(person.getId()))
                .thenReturn(List.of()); // empty

        List<AccountsResponseDTO> accountsResponseDTO = accountService.accounts(person.getEmail());

        assertEquals(0, accountsResponseDTO.size());
    }

    // --------------------------------------------
    // SUCCESS
    // -------------------------------------------

    @Test
    void delete_ShouldSuccess(){
        when(accountRepository.findById(account.getId()))
                .thenReturn(Optional.of(account));
        when(cardRepository.existsCardByAccountId(account.getId()))
                .thenReturn(false);
        account.setBalance(BigDecimal.ZERO);

        accountService.delete(account.getId());

        verify(accountRepository).delete(account);
    }

    // --------------------------------------------
    // FAILED - When account not foud for id
    // -------------------------------------------

    @Test
    void delete_ShouldFail_WhenAccountNotFoundForId(){
        when(accountRepository.findById(account.getId()))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> accountService.delete(account.getId()));

        assertTrue(ex.getMessage().contains("Account not found for id"));
    }

    // --------------------------------------------
    // FAILED - When account has card
    // -------------------------------------------

    @Test
    void delete_ShouldFail_WhenAccountHasCard(){
        when(accountRepository.findById(account.getId()))
                .thenReturn(Optional.of(account));
        when(cardRepository.existsCardByAccountId(account.getId()))
                .thenReturn(true);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> accountService.delete(account.getId()));

        assertTrue(ex.getMessage().contains("Card not found"));
    }

    // --------------------------------------------
    // FAILED - When account has balance > 0
    // -------------------------------------------

    @Test
    void delete_ShouldFail_WhenAccountHasBalance(){
        when(accountRepository.findById(account.getId()))
                .thenReturn(Optional.of(account));
        when(cardRepository.existsCardByAccountId(account.getId()))
                .thenReturn(false);

        BalanceZeroException ex = assertThrows(BalanceZeroException.class,
                () -> accountService.delete(account.getId()));

        assertTrue(ex.getMessage().contains("Account balance must be 0"));
    }

}
