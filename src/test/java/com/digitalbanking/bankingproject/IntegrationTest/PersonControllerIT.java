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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    // --------------------------------------------
    // PUT /update — SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void update_ShouldReturn200_WhenUpdateSuccessful() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "firstName": "RaulUpdated",
                    "lastName": "MrnUpdated",
                    "email": "raul2@gmail.com",
                    "CNP": 12345,
                    "pwd": "%s"
                }
                """.formatted(personRequestDTO.pwd());

        mockMvc.perform(put("/users/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    // --------------------------------------------
    // PUT /update — FAILED
    // --------------------------------------------

    @Test
    //@WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void update_ShouldReturn401_WhenNotAuthenticated() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "firstName": "RaulUpdated",
                    "lastName": "MrnUpdated",
                    "email": "raul2@gmail.com",
                    "CNP": 12345,
                    "pwd": "%s"
                }
                """.formatted(personRequestDTO.pwd());

        mockMvc.perform(put("/users/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------
    // PUT /update — FAILED
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void update_ShouldReturn409_WhenExistsByCNP() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "firstName": "RaulUpdated",
                    "lastName": "MrnUpdated",
                    "email": "raul2@gmail.com",
                    "CNP": 123456789,
                    "pwd": "%s"
                }
                """.formatted(personRequestDTO.pwd());

        mockMvc.perform(put("/users/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // PUT /update — FAILED
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void update_ShouldReturn409_WhenExistsByEmail() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "firstName": "RaulUpdated",
                    "lastName": "MrnUpdated",
                    "email": "raul@gmail.com",
                    "CNP": 12345678,
                    "pwd": "%s"
                }
                """.formatted(personRequestDTO.pwd());

        mockMvc.perform(put("/users/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // GET /me — SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul@gmail.com", roles = {"CUSTOMER"})
    void me_ShouldReturn200_WhenUserExists() throws Exception{
        personRepository.save(person);

        mockMvc.perform(get("/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // --------------------------------------------
    // GET /me — FAILED
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul1@gmail.com", roles = {"CUSTOMER"})
    void me_ShouldReturn401_WhenNotAuthenticated() throws Exception{
        personRepository.save(person);

        mockMvc.perform(get("/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------
    // PATCH /{email}/roles — SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"MANAGER"})
    void assignRole_ShouldReturn200_WhenAssignRole() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "role": "EMPLOYER"
                }
                """;

        mockMvc.perform(patch("/users/{email}/roles", "raul@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    // --------------------------------------------
    // PATCH /{email}/roles — FAILED
    // --------------------------------------------

    @Test
    //@WithMockUser(username = "raul2@gmail.com", roles = {"MANAGER"})
    void assignRole_ShouldReturn401_WhenNotAuthenticated() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "role": "EMPLOYER"
                }
                """;

        mockMvc.perform(patch("/users/{email}/roles", "raul@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------
    // PATCH /{email}/roles — FAILED
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"CUSTOMER"})
    void assignRole_ShouldReturn403_WhenForbidden() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "role": "EMPLOYER"
                }
                """;

        mockMvc.perform(patch("/users/{email}/roles", "raul@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    // --------------------------------------------
    // PATCH /{email}/roles — FAILED
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"ADMIN"})
    void assignRole_ShouldReturn409_WhenAlreadyHasRole() throws Exception{
        personRepository.save(person);

        String body = """
                {
                    "role": "CUSTOMER"
                }
                """;

        mockMvc.perform(patch("/users/{email}/roles", "raul@gmail.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // DELETE /{personId}/delete — SUCCESS
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"ADMIN"})
    void delete_ShouldReturn200_WhenDeleteSuccess() throws Exception{
        personRepository.save(person);
        transactionLimitRepository.save(transactionLimit);

        mockMvc.perform(delete("/users/{personId}/delete", person.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // --------------------------------------------
    // DELETE /{personId}/delete — FAILED
    // --------------------------------------------

    @Test
    //@WithMockUser(username = "raul2@gmail.com", roles = {"ADMIN"})
    void delete_ShouldReturn401_WhenNotAuthenticated() throws Exception{
        personRepository.save(person);
        transactionLimitRepository.save(transactionLimit);

        mockMvc.perform(delete("/users/{personId}/delete", person.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------
    // DELETE /{personId}/delete — FAILED
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"CUSTOMER"})
    void delete_ShouldReturn403_WhenForbidden() throws Exception{
        personRepository.save(person);
        transactionLimitRepository.save(transactionLimit);

        mockMvc.perform(delete("/users/{personId}/delete", person.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // --------------------------------------------
    // DELETE /{personId}/delete — FAILED
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"ADMIN"})
    void delete_ShouldReturn409_WhenPersonNotFoundById() throws Exception{
        personRepository.save(person);
        transactionLimitRepository.save(transactionLimit);

        mockMvc.perform(delete("/users/{personId}/delete", 2L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    // --------------------------------------------
    // DELETE /{personId}/delete — FAILED
    // --------------------------------------------

    @Test
    @WithMockUser(username = "raul2@gmail.com", roles = {"ADMIN"})
    void delete_ShouldReturn409_WhenTransactionLimitNotFound() throws Exception{
        personRepository.save(person);
        //transactionLimitRepository.save(transactionLimit);

        mockMvc.perform(delete("/users/{personId}/delete", person.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }
}
