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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class AccountControllerIT {

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
    // POST /register — SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void register_ShouldReturn200_WhenRegistrationSuccess() throws Exception {
        personRepository.save(person);

        String body = """
                {
                "currency": "EU"
                }
                """;

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    // --------------------------------------------
    // POST /register — FAILED - When not connected
    // --------------------------------------------

    @Test
    //@WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void register_ShouldReturn401_WhenNotConnected() throws Exception {
        personRepository.save(person);

        String body = """
                {
                "currency": "EU"
                }
                """;

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------
    // POST /register — FAILED - When person  not found
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void register_ShouldReturn409_WhenPersonNotFound() throws Exception {
        //personRepository.save(person);

        String body = """
                {
                "currency": "EU"
                }
                """;

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // POST /register — FAILED - When already has account in specified currency
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void register_ShouldReturn409_WhenHasAccount() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);

        String body = """
                {
                "currency": "RO"
                }
                """;

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // GET /accounts — SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void accounts_ShouldReturn200_WhenHasAccounts() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);

        mockMvc.perform(get("/accounts/myAccounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // --------------------------------------------
    // GET /accounts — FAILED - When person is not connected
    // --------------------------------------------

    @Test
    //@WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void accounts_ShouldReturn401_WhenPersonNotConnected() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);

        mockMvc.perform(get("/accounts/myAccounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------
    // DELETE /{accountId}/delete — SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"ADMIN"})
    void delete_ShouldReturn200_WhenAccountDelete() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        account.setBalance(BigDecimal.ZERO);

        mockMvc.perform(delete("/accounts/{accountId}/delete", account.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // --------------------------------------------
    // DELETE /{accountId}/delete — FAILED - When has no role
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"CUSTOMER"})
    void delete_ShouldReturn403_WhenAccountConnectedHasNoRole() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        account.setBalance(BigDecimal.ZERO);

        mockMvc.perform(delete("/accounts/{accountId}/delete", account.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // --------------------------------------------
    // DELETE /{accountId}/delete — FAILED - When has no account
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"ADMIN"})
    void delete_ShouldReturn409_WhenHasNoAccount() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        account.setBalance(BigDecimal.ZERO);

        mockMvc.perform(delete("/accounts/{accountId}/delete", 4)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // DELETE /{accountId}/delete — FAILED - When has balance
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"ADMIN"})
    void delete_ShouldReturn409_WhenHasBalance() throws Exception {
        personRepository.save(person);
        accountRepository.save(account);
        //account.setBalance(BigDecimal.ZERO);
        //cardRepository.save(card);

        mockMvc.perform(delete("/accounts/{accountId}/delete", person.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }
}
