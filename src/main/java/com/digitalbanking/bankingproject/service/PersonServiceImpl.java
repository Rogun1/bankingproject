package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.constants.PersonAccountStatus;
import com.digitalbanking.bankingproject.constants.PersonRole;
import com.digitalbanking.bankingproject.dto.PersonRequestDTO;
import com.digitalbanking.bankingproject.dto.PersonResponseDTO;
import com.digitalbanking.bankingproject.dto.PersonRoleSetDTO;
import com.digitalbanking.bankingproject.exceptions.InvalidAccountUsageException;
import com.digitalbanking.bankingproject.exceptions.NotFoundException;
import com.digitalbanking.bankingproject.exceptions.AlreadyExistsException;
import com.digitalbanking.bankingproject.exceptions.PersonAlreadyHasRoleException;
import com.digitalbanking.bankingproject.model.Account;
import com.digitalbanking.bankingproject.model.Authority;
import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.model.TransactionLimit;
import com.digitalbanking.bankingproject.repository.AccountRepository;
import com.digitalbanking.bankingproject.repository.AuthorityRepository;
import com.digitalbanking.bankingproject.repository.PersonRepository;
import com.digitalbanking.bankingproject.repository.TransactionLimitRepository;
import com.digitalbanking.bankingproject.service.declarations.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;
    private final TransactionLimitRepository transactionLimitRepository;
    private final AccountServiceImpl accountServiceImpl;
    private final AccountRepository accountRepository;

    @Autowired
    public PersonServiceImpl(
            PersonRepository personRepository,
            PasswordEncoder passwordEncoder,
            AuthorityRepository authorityRepository,
            TransactionLimitRepository transactionLimitRepository,
            AccountServiceImpl accountServiceImpl,
            AccountRepository accountRepository){
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.transactionLimitRepository = transactionLimitRepository;
        this.accountServiceImpl = accountServiceImpl;
        this.accountRepository = accountRepository;
    }


    @Override
    public PersonResponseDTO register(PersonRequestDTO personRequestDTO) {

        Boolean userExists = personRepository.existsByCNP(personRequestDTO.CNP());
        Boolean emailIsUsed = personRepository.existsByEmail(personRequestDTO.email());

        if (userExists){
            throw new AlreadyExistsException("User already exists");
        }
        if (emailIsUsed){
            throw new AlreadyExistsException("Email already used");
        }

        String hashPwd = passwordEncoder.encode(personRequestDTO.pwd());

        Person person = new Person(
                null,
                personRequestDTO.email(),
                hashPwd,
                personRequestDTO.firstName(),
                personRequestDTO.lastName(),
                personRequestDTO.CNP(),
                null,
                PersonAccountStatus.ACTIVE,
                new Date()
        );

        Set<Authority> authorities = Set.of(new Authority(
                null,
                "ROLE_" + PersonRole.CUSTOMER,
                person
        ));

        TransactionLimit transactionLimit = new TransactionLimit(
                null,
                person,
                12500.0,
                5000.0,
                5
        );
        person.setAuthorities(authorities);
        personRepository.save(person);
        transactionLimitRepository.save(transactionLimit);

        return toDTO(person);
    }

    //After connection, it gets the JWT token
    @Override
    public PersonResponseDTO login(Authentication authentication){
        Optional<Person> person = personRepository.findByEmail(authentication.getName());

        return toDTO(person.orElse(null));
    }

    @Override
    public PersonResponseDTO update(String email, PersonRequestDTO personRequestDTO){

        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account doesn't exist for this email"));
        String hashPwd = passwordEncoder.encode(personRequestDTO.pwd());

        Boolean userExists = personRepository.existsByCNP(personRequestDTO.CNP());
        Boolean emailExists = personRepository.existsByEmail(personRequestDTO.email());

        if (emailExists.equals(email)){
            emailExists = false;
        }

        if (userExists){
            throw new AlreadyExistsException("User already exists");
        }
        if (emailExists){
            throw new AlreadyExistsException("Email already used");
        }

        person.setFirstName(personRequestDTO.firstName());
        person.setLastName(personRequestDTO.lastName());
        person.setEmail(personRequestDTO.email());
        person.setCNP(personRequestDTO.CNP());
        person.setPwd(hashPwd);

        return toDTO(personRepository.save(person));
    }

    @Override
    public PersonResponseDTO profile(String email){
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidAccountUsageException("User doesn't exists for email : " + email));

        return toDTO(person);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Override
    public PersonResponseDTO assignRole(PersonRoleSetDTO roleDTO, String email){
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidAccountUsageException("Person doesn't exist for email : " + email));
        Set<Authority> authorities = authorityRepository.findAllByPersonId(person.getId());

        PersonRole role;

        try {
            role = PersonRole.valueOf(roleDTO.role().toUpperCase());
        }catch (Exception e){
            throw new RuntimeException("Invalid role: " + roleDTO.role());
        }

        String roleName = "ROLE_" + roleDTO.role();

        Boolean existsRole = person.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getName().equals(roleName));

        if (existsRole){
            throw new PersonAlreadyHasRoleException("User already has role: " + roleName);
        }

        Authority authority = new Authority(
                null,
                roleName,
                person
        );

        person.getAuthorities().add(authority);

        return toDTO(personRepository.save(person));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Override
    public void delete(Long personId){
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new NotFoundException("User not found for id: " + personId));

        Set<Authority> authorities = authorityRepository.findAllByPersonId(personId);

        TransactionLimit transactionLimit = transactionLimitRepository.findByPersonId(personId)
                .orElseThrow(() -> new NotFoundException("No  limits for user: " + personId));

        List<Account> accounts = accountRepository.findAllByPersonId(personId);

        for (Account a : accounts){
            accountServiceImpl.delete(a.getId());
        }

        for (Authority a : authorities){
            authorityRepository.delete(a);
        }

        transactionLimitRepository.delete(transactionLimit);
        personRepository.delete(person);
    }
}
