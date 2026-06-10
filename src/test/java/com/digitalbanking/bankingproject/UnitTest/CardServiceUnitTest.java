package com.digitalbanking.bankingproject.UnitTest;

import com.digitalbanking.bankingproject.constants.*;
import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.CardResponseDTO;
import com.digitalbanking.bankingproject.dto.CardStatusBlockRequestDTO;
import com.digitalbanking.bankingproject.dto.PersonRequestDTO;
import com.digitalbanking.bankingproject.exceptions.AlreadyExistsException;
import com.digitalbanking.bankingproject.exceptions.InvalidRequestException;
import com.digitalbanking.bankingproject.exceptions.NotFoundException;
import com.digitalbanking.bankingproject.model.*;
import com.digitalbanking.bankingproject.repository.*;
import com.digitalbanking.bankingproject.service.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CardServiceUnitTest {

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
    private CardServiceImpl cardService;

    private Person person;
    private PersonRequestDTO personRequestDTO;
    private String hashedPwd;
    private TransactionLimit transactionLimit;
    private AccountRequestDTO accountRequestDTO;
    private Account account;
    private Card card;
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

        int year = LocalDate.now().getYear() + 7;
        int month = LocalDate.now().getMonthValue();
        int day = LocalDate.now().getDayOfMonth();
        LocalDate expDate = LocalDate.of(year,month,day);

        card = new Card(
                null,
                account,
                "1234",
                1234,
                expDate,
                CardStatus.ONHOLD,
                2000,
                new Date()
        );
    }

    // --------------------------------------------
    // SUCCESS
    // --------------------------------------------

    @Test
    void getCard_ShouldSuccess_WhenCardCreated() throws Exception {
        when(personRepository.findByEmail(emailJWT))
                .thenReturn(Optional.of(person));
        when(accountRepository.findAllByPersonId(person.getId()))
                .thenReturn(List.of(account));
        when(cardRepository.existsCardByAccountId(account.getId()))
                .thenReturn(false);
        when(cardRepository.save(any(Card.class)))
                .thenReturn(card);

        CardResponseDTO responseDTO = cardService.getCard(emailJWT,accountRequestDTO);

        assertEquals(account.getId(),card.getAccount().getId());
        assertEquals("1234", responseDTO.cardNumber());
        verify(cardRepository).save(any(Card.class));
    }

    // --------------------------------------------
    // FAILED - When person not found
    // --------------------------------------------

    @Test
    void getCard_ShouldFail_WhenPersonNotFound() throws Exception {
        when(personRepository.findByEmail(emailJWT))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> cardService.getCard(emailJWT, accountRequestDTO));

        assertTrue(ex.getMessage().contains("User doesn't exists for email"));
    }

    // --------------------------------------------
    // FAILED - When already has card
    // --------------------------------------------

    @Test
    void getCard_ShouldFail_WhenHasCard() throws Exception {
        when(personRepository.findByEmail(emailJWT))
                .thenReturn(Optional.of(person));
        when(accountRepository.findAllByPersonId(person.getId()))
                .thenReturn(List.of(account));
        when(cardRepository.existsCardByAccountId(account.getId()))
                .thenReturn(true);

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> cardService.getCard(emailJWT, accountRequestDTO));

        assertTrue(ex.getMessage().contains("You already have an card"));
    }

    // --------------------------------------------
    // SUCCESS
    // --------------------------------------------

    @Test
    void blockCard_ShouldSuccess_WhenCardIsBlocked(){
        CardStatusBlockRequestDTO cardStatusBlockRequestDTO = new CardStatusBlockRequestDTO("BLOCKED");

        when(cardRepository.findById(card.getId()))
                .thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class)))
                .thenReturn(card);

        CardResponseDTO responseDTO = cardService.blockCard(emailJWT, card.getId(), cardStatusBlockRequestDTO);

        assertEquals(card.getStatus(), responseDTO.cardStatus());
        verify(cardRepository).save(any(Card.class));
    }

    // --------------------------------------------
    // FAILED - When card not found
    // --------------------------------------------

    @Test
    void blockCard_ShouldFail_WhenCardNotFound(){
        CardStatusBlockRequestDTO cardStatusBlockRequestDTO = new CardStatusBlockRequestDTO("BLOCKED");

        when(cardRepository.findById(card.getId()))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> cardService.blockCard(emailJWT, card.getId(), cardStatusBlockRequestDTO));

        assertTrue(ex.getMessage().contains("Card not found"));
    }

    // --------------------------------------------
    // FAILED - When card is already blocked
    // --------------------------------------------

    @Test
    void blockCard_ShouldFail_WhenCardIsAlreadyBlocked(){
        CardStatusBlockRequestDTO cardStatusBlockRequestDTO = new CardStatusBlockRequestDTO("BLOCKED");
        card.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(card.getId()))
                .thenReturn(Optional.of(card));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> cardService.blockCard(emailJWT, card.getId(), cardStatusBlockRequestDTO));

        assertTrue(ex.getMessage().contains("already blocked"));
    }

    // --------------------------------------------
    // FAILED - When status from request is invalid
    // --------------------------------------------

    @Test
    void blockCard_ShouldFail_WhenRequestStatusIsInvalid(){
        CardStatusBlockRequestDTO cardStatusBlockRequestDTO = new CardStatusBlockRequestDTO("INVALID");
        when(cardRepository.findById(card.getId()))
                .thenReturn(Optional.of(card));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> cardService.blockCard(emailJWT, card.getId(), cardStatusBlockRequestDTO));

        assertTrue(ex.getMessage().contains("Invalid status"));
    }

}
