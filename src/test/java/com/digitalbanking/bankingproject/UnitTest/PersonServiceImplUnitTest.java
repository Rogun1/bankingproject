package com.digitalbanking.bankingproject.UnitTest;

import com.digitalbanking.bankingproject.constants.PersonAccountStatus;
import com.digitalbanking.bankingproject.constants.PersonRole;
import com.digitalbanking.bankingproject.dto.PersonRequestDTO;
import com.digitalbanking.bankingproject.dto.PersonResponseDTO;
import com.digitalbanking.bankingproject.dto.PersonRoleSetDTO;
import com.digitalbanking.bankingproject.model.Account;
import com.digitalbanking.bankingproject.model.Authority;
import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.model.TransactionLimit;
import com.digitalbanking.bankingproject.repository.AccountRepository;
import com.digitalbanking.bankingproject.repository.AuthorityRepository;
import com.digitalbanking.bankingproject.repository.PersonRepository;
import com.digitalbanking.bankingproject.repository.TransactionLimitRepository;
import com.digitalbanking.bankingproject.service.AccountServiceImpl;
import com.digitalbanking.bankingproject.service.PersonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PersonServiceImplUnitTest {

    @Mock
    private PersonRepository personRepository;
    @Mock
    private AuthorityRepository authorityRepository;
    @Mock
    private TransactionLimitRepository transactionLimitRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountServiceImpl accountServiceImpl;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private PersonServiceImpl personService;

    private Person person;
    private PersonRequestDTO personRequestDTO;
    private String hashedPwd;
    private TransactionLimit transactionLimit;

    @BeforeEach
    void setup() {
        personRequestDTO = new PersonRequestDTO(
                "Raul",
                "Mrn",
                "raul@gmail.com",
                123456789L,
                "Parolae@123"
        );

        hashedPwd = passwordEncoder.encode(personRequestDTO.pwd());

        person = new Person(
                1L,
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
                        1L,
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
    }

    // --------------------------------------------
    // SUCCESS
    // --------------------------------------------

    @Test
    void register_ShouldSavePerson() {
        when(personRepository.existsByCNP(personRequestDTO.CNP()))
                .thenReturn(false);
        when(personRepository.existsByEmail(personRequestDTO.email()))
                .thenReturn(false);
        when(passwordEncoder.encode(personRequestDTO.pwd()))
                .thenReturn(hashedPwd);
        when(personRepository.save(any(Person.class)))
                .thenReturn(person);
        when(transactionLimitRepository.save(any(TransactionLimit.class)))
                .thenReturn(transactionLimit);

        PersonResponseDTO responseDTO = personService.register(personRequestDTO);

        assertEquals("Raul Mrn", responseDTO.userName());
        assertEquals("raul@gmail.com", responseDTO.email());
        assertEquals("ACTIVE", responseDTO.status());
        verify(personRepository).save(any(Person.class));
        verify(transactionLimitRepository).save(any(TransactionLimit.class));
    }

    // --------------------------------------------
    // CNP ALREADY EXISTS
    // --------------------------------------------

    @Test
    void register_ShouldThrow_WhenCNPAlreadyExists() {
        when(personRepository.existsByCNP(personRequestDTO.CNP())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.register(personRequestDTO));

        assertEquals("User already exists", ex.getMessage());
        verify(personRepository, never()).save(any());
    }

    // --------------------------------------------
    // EMAIL ALREADY EXISTS
    // --------------------------------------------

    @Test
    void register_ShouldThrow_WhenEmailAlreadyUsed() {
        when(personRepository.existsByCNP(personRequestDTO.CNP()))
                .thenReturn(false);
        when(personRepository.existsByEmail(personRequestDTO.email()))
                .thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.register(personRequestDTO));

        assertEquals("Email already used", ex.getMessage());
        verify(personRepository, never()).save(any());
    }

    // --------------------------------------------
    // PERSON PROFILE EXISTS
    // --------------------------------------------

    @Test
    void profile_ShouldReturnPersonDTO_WhenUserExists() {
        when(personRepository.findByEmail("raul@gmail.com"))
                .thenReturn(Optional.of(person));

        PersonResponseDTO responseDTO = personService.profile("raul@gmail.com");

        assertNotNull(responseDTO);
        assertEquals("raul@gmail.com", responseDTO.email());
        assertEquals("Raul Mrn", responseDTO.userName());
    }

    // --------------------------------------------
    // PERSON PROFILE DOESN'T
    // --------------------------------------------

    @Test
    void profile_ShouldThrow_WhenUserNotFound() {
        when(personRepository.findByEmail("missing@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.profile("missing@gmail.com"));

        assertTrue(ex.getMessage().contains("missing@gmail.com"));
    }

    // --------------------------------------------
    // UPDATE PERSON SUCCESS
    // --------------------------------------------

    @Test
    void update_ShouldUpdatePerson_WhenDataIsValid() {
        PersonRequestDTO updateDTO = new PersonRequestDTO(
                "RaulUpdated",
                "MrnUpdated",
                "raul.updated@gmail.com",
                987654321L,
                "NewPassword@123"
        );

        hashedPwd = passwordEncoder.encode(updateDTO.pwd());

        when(personRepository.findByEmail("raul@gmail.com"))
                .thenReturn(Optional.of(person));
        when(personRepository.existsByCNP(updateDTO.CNP()))
                .thenReturn(false);
        when(personRepository.existsByEmail(updateDTO.email()))
                .thenReturn(false);
        when(passwordEncoder.encode(updateDTO.pwd()))
                .thenReturn(hashedPwd);
        // When save() gets any Person class, then return first argument, if multiple (1),(2), etc.
        // Instead of .thenReturn on save() witch will return the person from @BeforeEach
        // We return the exact object we saved with .thenAnswer...
        when(personRepository.save(any(Person.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PersonResponseDTO responseDTO = personService.update(
                "raul@gmail.com",
                updateDTO);

        assertEquals("RaulUpdated MrnUpdated", responseDTO.userName());
        assertEquals("raul.updated@gmail.com", responseDTO.email());
        verify(personRepository).save(any(Person.class));
    }

    // --------------------------------------------
    // UPDATE PERSON NOT FOUND EMAIL
    // --------------------------------------------

    @Test
    void update_ShouldThrow_WhenEmailNotFound() {
        when(personRepository.findByEmail("nobody@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.update("nobody@gmail.com", personRequestDTO));

        assertEquals("Account doesn't exist for this email", ex.getMessage());
    }

    // --------------------------------------------
    // UPDATE PERSON CNP EXISTS
    // --------------------------------------------

    @Test
    void update_ShouldThrow_WhenNewCNPAlreadyExists() {
        PersonRequestDTO updateDTO = new PersonRequestDTO(
                "Raul",
                "Mrn",
                "other@gmail.com",
                999999999L,
                "Pass@123"
        );

        when(personRepository.findByEmail("raul@gmail.com"))
                .thenReturn(Optional.of(person));
        when(personRepository.existsByCNP(updateDTO.CNP()))
                .thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.update("raul@gmail.com", updateDTO));

        assertEquals("User already exists", ex.getMessage());
    }

    // --------------------------------------------
    // UPDATE PERSON EMAILED ALREADY USED
    // --------------------------------------------

    @Test
    void update_ShouldThrow_WhenNewEmailAlreadyUsed() {
        PersonRequestDTO updateDTO = new PersonRequestDTO(
                "Raul",
                "Mrn",
                "taken@gmail.com",
                111111111L,
                "Pass@123"
        );

        when(personRepository.findByEmail("raul@gmail.com"))
                .thenReturn(Optional.of(person));
        when(personRepository.existsByCNP(updateDTO.CNP()))
                .thenReturn(false);
        when(personRepository.existsByEmail(updateDTO.email()))
                .thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.update("raul@gmail.com", updateDTO));

        assertEquals("Email already used", ex.getMessage());
    }

    // --------------------------------------------
    // ASSIGN ROLE SUCCESS
    // --------------------------------------------

    @Test
    void assignRole_ShouldAddNewRole_WhenRoleIsValid() {
        PersonRoleSetDTO roleDTO = new PersonRoleSetDTO("MANAGER");

        when(personRepository.findByEmail("raul@gmail.com"))
                .thenReturn(Optional.of(person));
        when(authorityRepository.findAllByPersonId(person.getId()))
                .thenReturn(person.getAuthorities());
        when(personRepository.save(any(Person.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PersonResponseDTO responseDTO = personService.assignRole(roleDTO, "raul@gmail.com");

        assertNotNull(responseDTO);
        assertTrue(person.getAuthorities().stream()
                .anyMatch(a -> a.getName().equals("ROLE_MANAGER")));
        verify(personRepository).save(any(Person.class));
    }

    // --------------------------------------------
    // ASSIGN ROLE FAILED WHEN EMAIL DOESN'T EXISTS
    // --------------------------------------------

    @Test
    void assignRole_ShouldThrow_WhenUserNotFound() {
        PersonRoleSetDTO roleDTO = new PersonRoleSetDTO("ADMIN");

        when(personRepository.findByEmail("nobody@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.assignRole(roleDTO, "nobody@gmail.com"));

        assertTrue(ex.getMessage().contains("nobody@gmail.com"));
    }

    // --------------------------------------------
    // ASSIGN ROLE FAILED WHEN ROLE IS INVALID
    // --------------------------------------------

    @Test
    void assignRole_ShouldThrow_WhenRoleIsInvalid() {
        PersonRoleSetDTO roleDTO = new PersonRoleSetDTO("SUPERUSER");

        when(personRepository.findByEmail("raul@gmail.com"))
                .thenReturn(Optional.of(person));
        when(authorityRepository.findAllByPersonId(person.getId()))
                .thenReturn(person.getAuthorities());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.assignRole(roleDTO, "raul@gmail.com"));

        assertTrue(ex.getMessage().contains("Invalid role"));
    }

    // --------------------------------------------
    // ASSIGN ROLE FAILED WHEN HAS ROLE ALREADY
    // --------------------------------------------

    @Test
    void assignRole_ShouldThrow_WhenRoleAlreadyAssigned() {
        PersonRoleSetDTO roleDTO = new PersonRoleSetDTO("CUSTOMER");

        when(personRepository.findByEmail("raul@gmail.com"))
                .thenReturn(Optional.of(person));
        when(authorityRepository.findAllByPersonId(person.getId()))
                .thenReturn(person.getAuthorities());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.assignRole(roleDTO, "raul@gmail.com"));

        assertTrue(ex.getMessage().contains("already has role"));
    }

    // --------------------------------------------
    // DELETE SUCCESS
    // --------------------------------------------

    @Test
    void delete_ShouldDeletePerson_WhenExists() {
        Long personId = 1L;
        Account account = new Account();
        account.setId(10L);

        when(personRepository.findById(personId))
                .thenReturn(Optional.of(person));
        when(authorityRepository.findAllByPersonId(personId))
                .thenReturn(person.getAuthorities());
        when(transactionLimitRepository.findByPersonId(personId))
                .thenReturn(Optional.of(transactionLimit));
        when(accountRepository.findAllByPersonId(personId))
                .thenReturn(List.of(account));

        personService.delete(personId);

        verify(accountServiceImpl).delete(10L);
        verify(authorityRepository, atLeastOnce()).delete(any(Authority.class));
        verify(transactionLimitRepository).delete(transactionLimit);
        verify(personRepository).delete(person);
    }

    // --------------------------------------------
    // DELETE FAILED WHEN USER NOT FOUND
    // --------------------------------------------

    @Test
    void delete_ShouldThrow_WhenPersonNotFound() {
        when(personRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.delete(99L));

        assertTrue(ex.getMessage().contains("99"));
        verify(personRepository, never()).delete(any(Person.class));
    }

    // --------------------------------------------
    // DELETE FAILED WHEN TRANSACTION LIMIT DOESN'T EXISTS
    // --------------------------------------------

    @Test
    void delete_ShouldThrow_WhenTransactionLimitNotFound() {
        Long personId = 1L;

        when(personRepository.findById(personId))
                .thenReturn(Optional.of(person));
        when(authorityRepository.findAllByPersonId(personId))
                .thenReturn(person.getAuthorities());
        when(transactionLimitRepository.findByPersonId(personId))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> personService.delete(personId));

        assertTrue(ex.getMessage().contains("limits"));
        verify(personRepository, never()).delete(any(Person.class));
    }
}