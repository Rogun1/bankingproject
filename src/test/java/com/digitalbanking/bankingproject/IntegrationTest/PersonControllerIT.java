package com.digitalbanking.bankingproject.IntegrationTest;

import com.digitalbanking.bankingproject.constants.PersonAccountStatus;
import com.digitalbanking.bankingproject.constants.PersonRole;
import com.digitalbanking.bankingproject.dto.PersonRequestDTO;
import com.digitalbanking.bankingproject.model.Authority;
import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.model.TransactionLimit;
import com.digitalbanking.bankingproject.repository.AuthorityRepository;
import com.digitalbanking.bankingproject.repository.PersonRepository;
import com.digitalbanking.bankingproject.repository.TransactionLimitRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class PersonControllerIT {

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

    private Person person;
    private PersonRequestDTO personRequestDTO;
    private String hashedPwd;
    private TransactionLimit transactionLimit;
    private Set<Authority> authorities;

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

        authorities = Set.of(new Authority(
                null,
                "ROLE_" + PersonRole.CUSTOMER,
                person
        ));

        person.setAuthorities(authorities);


        transactionLimit = new TransactionLimit(
                null,
                person,
                2000.0,
                500.0,
                5
        );
    }

    // --------------------------------------------
    // POST /register — SUCCESS
    // --------------------------------------------

    @Test
    void register_ShouldReturn200_WhenRegistrationIsSuccessful() throws Exception{
        String body = """
                {
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "CNP": %d,
                    "pwd": "%s"
                }
                """.formatted(
                        personRequestDTO.firstName(),
                        personRequestDTO.lastName(),
                        personRequestDTO.email(),
                        personRequestDTO.CNP(),
                        personRequestDTO.pwd());

        mockMvc.perform(post("/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk());
    }

    // --------------------------------------------
    // POST /register — FAILED - WHEN USER EXISTS BY CNP
    // --------------------------------------------

    @Test
    void register_ShouldReturn400_WhenUserAlreadyExistsByCNP() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "CNP": 123456789L,
                    "pwd": "%s"
                }
                """.formatted(
                personRequestDTO.firstName(),
                personRequestDTO.lastName(),
                personRequestDTO.email(),
                personRequestDTO.pwd());

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().is4xxClientError());
    }

    // --------------------------------------------
    // POST /register — FAILED - WHEN EMAIL ALREADY EXISTS
    // --------------------------------------------

    @Test
    void register_ShouldReturn400_WhenEmailAlreadyExists() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "raul@gmail.com",
                    "CNP": 1234L,
                    "pwd": "%s"
                }
                """.formatted(
                personRequestDTO.firstName(),
                personRequestDTO.lastName(),
                personRequestDTO.pwd());

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().is4xxClientError());
    }
}
