package com.digitalbanking.bankingproject.IntegrationTest;

import com.digitalbanking.bankingproject.constants.AccountStatus;
import com.digitalbanking.bankingproject.constants.AccountType;
import com.digitalbanking.bankingproject.constants.PersonAccountStatus;
import com.digitalbanking.bankingproject.model.Account;
import com.digitalbanking.bankingproject.model.Authority;
import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.model.TransactionLimit;
import com.digitalbanking.bankingproject.repository.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class TransactionControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionLimitRepository transactionLimitRepository;
    @Autowired
    private WebApplicationContext webApplicationContext;

    private Person personFrom;
    private Person personTo;
    private Account accountFrom;
    private Account accountTo;
    private TransactionLimit transactionLimit;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        personFrom = new Person(
                null,
                "raul@gmail.com",
                "Parola1234@asd",
                "Raul",
                "Mrn",
                123456789L,
                null,
                PersonAccountStatus.ONHOLD,
                new Date());
        personTo = new Person(
                null,
                "alex@gmail.com",
                "Parola1234@asd",
                "Alex",
                "Nasa",
                223456789L,
                null,
                PersonAccountStatus.ONHOLD,
                new Date());

        Authority authorityFrom = new Authority(
                null,
                "CUSTOMER",
                personFrom);
        personFrom.setAuthorities(Set.of(authorityFrom));

        Authority authorityTo = new Authority(
                null,
                "CUSTOMER",
                personTo);
        personTo.setAuthorities(Set.of(authorityTo));

        accountFrom = new Account(
                null,
                personFrom,
                "RO",
                "WHATEVER",
                BigDecimal.valueOf(30),
                AccountType.CURRENT,
                AccountStatus.ONHOLD,
                new Date());
        accountTo = new Account(
                null,
                personTo,
                "RO",
                "WHATEVER_TWO",
                BigDecimal.ZERO,
                AccountType.CURRENT,
                AccountStatus.ONHOLD,
                new Date());

        transactionLimit = new TransactionLimit(
                null,
                personFrom,
                1000.0,
                1000.0,
                10);
    }

    // --------------------------------------------
    // POST /transactions — TRANSFER - SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void transfer_ShouldReturn200_WhenTransferIsSuccessful() throws Exception {
        personFrom = personRepository.save(personFrom);
        personTo = personRepository.save(personTo);
        accountFrom = accountRepository.save(accountFrom);
        accountTo = accountRepository.save(accountTo);
        transactionLimitRepository.save(transactionLimit);

        String body = """
                {
                    "fromAccountId": %d,
                    "toAccountId": %d,
                    "amount": 20,
                    "description": "something"
                }
                """.formatted(accountFrom.getId(), accountTo.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAccountId").value(accountFrom.getId()))
                .andExpect(jsonPath("$.toAccountId").value(accountTo.getId()))
                .andExpect(jsonPath("$.amount").value(20))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }


    // --------------------------------------------
    // 401 - BAD REQUEST
    // --------------------------------------------
    @Test
    void transfer_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        String body = """
                {
                    "fromAccountId": 1,
                    "toAccountId": 2,
                    "amount": 20,
                    "description": "something"
                }
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------
    // 409 - CONFLICT
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void transfer_ShouldReturn409_WhenFromAndToAccountAreSame() throws Exception {
        personFrom = personRepository.save(personFrom);
        accountFrom = accountRepository.save(accountFrom);
        transactionLimitRepository.save(transactionLimit);

        String body = """
                {
                    "fromAccountId": %d,
                    "toAccountId": %d,
                    "amount": 20,
                    "description": "something"
                }
                """.formatted(accountFrom.getId(), accountFrom.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // 401 - UNAUTHORIZE
    // --------------------------------------------
    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"}) // Logged in with raul@gmail.com
    void transfer_ShouldReturn401_WhenFromAccountBelongsToAnotherUser() throws Exception {
        // accountFrom belongs to personTwo which is not raul@gmail.com
        accountFrom.setPerson(personTo);

        personFrom = personRepository.save(personFrom);
        personTo = personRepository.save(personTo);
        accountFrom = accountRepository.save(accountFrom);
        accountTo = accountRepository.save(accountTo);
        transactionLimitRepository.save(transactionLimit);

        String body = """
                {
                    "fromAccountId": %d,
                    "toAccountId": %d,
                    "amount": 20,
                    "description": "something"
                }
                """.formatted(accountFrom.getId(), accountTo.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------
    // 409 - CONFLICT
    // --------------------------------------------
    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void transfer_ShouldReturn409_WhenFromAccountIsDisabled() throws Exception {
        accountFrom.setStatus(AccountStatus.DISABLED);

        personFrom = personRepository.save(personFrom);
        personTo = personRepository.save(personTo);
        accountFrom = accountRepository.save(accountFrom);
        accountTo = accountRepository.save(accountTo);
        transactionLimitRepository.save(transactionLimit);

        String body = """
                {
                    "fromAccountId": %d,
                    "toAccountId": %d,
                    "amount": 20,
                    "description": "something"
                }
                """.formatted(accountFrom.getId(), accountTo.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                        .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // 409 - CONFLICT
    // --------------------------------------------
    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void transfer_ShouldReturn409_WhenToAccountIsDisabled() throws Exception {
        accountTo.setStatus(AccountStatus.DISABLED);

        personFrom = personRepository.save(personFrom);
        personTo = personRepository.save(personTo);
        accountFrom = accountRepository.save(accountFrom);
        accountTo = accountRepository.save(accountTo);
        transactionLimitRepository.save(transactionLimit);

        String body = """
                {
                    "fromAccountId": %d,
                    "toAccountId": %d,
                    "amount": 20,
                    "description": "something"
                }
                """.formatted(accountFrom.getId(), accountTo.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // 409 - CONFLICT
    // --------------------------------------------
    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void transfer_ShouldReturn409_WhenInsufficientFunds() throws Exception {
        accountFrom.setBalance(BigDecimal.ONE);

        personFrom = personRepository.save(personFrom);
        personTo = personRepository.save(personTo);
        accountFrom = accountRepository.save(accountFrom);
        accountTo = accountRepository.save(accountTo);
        transactionLimitRepository.save(transactionLimit);

        String body = """
                {
                    "fromAccountId": %d,
                    "toAccountId": %d,
                    "amount": 20,
                    "description": "something"
                }
                """.formatted(accountFrom.getId(), accountTo.getId());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // GET /transactions - transactionsInRangeForUser
    // --------------------------------------------
    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void transactionsInRange_ShouldReturn200_WhenNoTransactionsExist() throws Exception {
        personFrom = personRepository.save(personFrom);
        accountFrom = accountRepository.save(accountFrom);

        String body = """
                {
                    "dateFrom": "2024-01-01",
                    "dateTo": "2024-12-31"
                }
                """;

        mockMvc.perform(get("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --------------------------------------------
    // GET /transactions - transactionsInRangeForUser
    // --------------------------------------------
    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void transactionsInRange_ShouldReturn200_WithTransactionsInRange() throws Exception {
        personFrom = personRepository.save(personFrom);
        personTo = personRepository.save(personTo);
        accountFrom = accountRepository.save(accountFrom);
        accountTo = accountRepository.save(accountTo);
        transactionLimitRepository.save(transactionLimit);

        String transferBody = """
                {
                    "fromAccountId": %d,
                    "toAccountId": %d,
                    "amount": 20,
                    "description": "something"
                }
                """.formatted(accountFrom.getId(), accountTo.getId());

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferBody));

        String rangeBody = """
                {
                    "dateFrom": "2020-01-01",
                    "dateTo": "2099-12-31"
                }
                """;

        mockMvc.perform(get("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rangeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].fromAccountId").value(accountFrom.getId()));
    }

    // --------------------------------------------
    // 400 - BAD REQUEST
    // --------------------------------------------
    @Test
    void transactionsInRange_ShouldReturn400_WhenNotAuthenticated() throws Exception {
        String body = """
                {
                    "dateFrom": "2024-01-01",
                    "dateTo": "2024-12-31"
                }
                """;

        mockMvc.perform(get("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}