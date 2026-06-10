package com.digitalbanking.bankingproject.IntegrationTest;

import com.digitalbanking.bankingproject.constants.*;
import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.PersonRequestDTO;
import com.digitalbanking.bankingproject.model.*;
import com.digitalbanking.bankingproject.repository.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class CardControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private TransactionLimitRepository transactionLimitRepository;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private AuthorityRepository authorityRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CardRepository cardRepository;

    private Person person;
    private PersonRequestDTO personRequestDTO;
    private String hashedPwd;
    private TransactionLimit transactionLimit;
    private Set<Authority> authorities;
    private Account account;
    private AccountRequestDTO accountRequestDTO;
    private Card card;

    @BeforeEach
    void setup(){
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

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

        authorities = new HashSet<>(Set.of(
                new Authority(
                        null,
                        "ROLE_" + PersonRole.CUSTOMER,
                        person
                )
        ));

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
                CardStatus.ACTIVE,
                2000,
                new Date()
        );
    }

    // --------------------------------------------
    // POST /accounts/cards — SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void getCard_ShouldReturn200_WhenCardIsCreated() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);

        String body = """
                {
                "currency": "RO"
                }
                """;

        mockMvc.perform(post("/accounts/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

    }

    // --------------------------------------------
    // POST /accounts/cards — FAILED - When not connected
    // --------------------------------------------

    @Test
    //@WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void getCard_ShouldReturn401_WhenNotConnected() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);

        String body = """
                {
                "currency": "RO"
                }
                """;

        mockMvc.perform(post("/accounts/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

    }

    // --------------------------------------------
    // POST /accounts/cards — FAILED - When card exists
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void getCard_ShouldReturn409_WhenCardExists() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        cardRepository.save(card);

        String body = """
                {
                "currency": "RO"
                }
                """;

        mockMvc.perform(post("/accounts/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

    }

    // --------------------------------------------
    // POST /accounts/cards — FAILED - When account doesn't match the card request
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void getCard_ShouldReturn409_WhenAccountAndCardDoNotMatch() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        cardRepository.save(card);

        String body = """
                {
                "currency": "EU"
                }
                """;

        mockMvc.perform(post("/accounts/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

    }

    // --------------------------------------------
    // Patch /accounts/cards/{cardId}/block — SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void blockCard_ShouldReturn200_WhenCardIsBlocked() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        cardRepository.save(card);

        String body = """
                {
                "cardStatus": "BLOCKED"
                }
                """;

        mockMvc.perform(patch("/accounts/cards/{cardId}/block", card.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

    }

    // --------------------------------------------
    // Patch /accounts/cards/{cardId}/block — FAILED - When not connected
    // --------------------------------------------

    @Test
    //@WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void blockCard_ShouldReturn401_WhenNotConnected() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        cardRepository.save(card);

        String body = """
                {
                "cardStatus": "BLOCKED"
                }
                """;

        mockMvc.perform(patch("/accounts/cards/{cardId}/block", card.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

    }

    // --------------------------------------------
    // Patch /accounts/cards/{cardId}/block — FAILED - Card is already blocked
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void blockCard_ShouldReturn409_WhenCardIsAlreadyBlocked() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        cardRepository.save(card);
        card.setStatus(CardStatus.BLOCKED);

        String body = """
                {
                "cardStatus": "BLOCKED"
                }
                """;

        mockMvc.perform(patch("/accounts/cards/{cardId}/block", card.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

    }

    // --------------------------------------------
    // Patch /accounts/cards/{cardId}/block — FAILED - Invalid Status
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void blockCard_ShouldReturn409_WhenCardStatusRequestIsInvalid() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        cardRepository.save(card);
        card.setStatus(CardStatus.BLOCKED);

        String body = """
                {
                "cardStatus": "INVALID"
                }
                """;

        mockMvc.perform(patch("/accounts/cards/{cardId}/block", card.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

    }

}
