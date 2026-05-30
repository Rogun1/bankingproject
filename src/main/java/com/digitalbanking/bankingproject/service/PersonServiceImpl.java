package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.constants.PersonAccountStatus;
import com.digitalbanking.bankingproject.constants.PersonRole;
import com.digitalbanking.bankingproject.dto.PersonRequestDTO;
import com.digitalbanking.bankingproject.dto.PersonResponseDTO;
import com.digitalbanking.bankingproject.dto.PersonRoleSetDTO;
import com.digitalbanking.bankingproject.model.Authority;
import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.model.TransactionLimit;
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
import java.util.Optional;
import java.util.Set;

@Service
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;
    private final TransactionLimitRepository transactionLimitRepository;

    @Autowired
    public PersonServiceImpl(
            PersonRepository personRepository,
            PasswordEncoder passwordEncoder,
            AuthorityRepository authorityRepository,
            TransactionLimitRepository transactionLimitRepository){
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.transactionLimitRepository = transactionLimitRepository;
    }


    @Override
    public PersonResponseDTO register(PersonRequestDTO personRequestDTO) {

        Boolean userExists = personRepository.existsByCNP(personRequestDTO.CNP());
        Boolean emailIsUsed = personRepository.existsByEmail(personRequestDTO.email());

        if (userExists){
            throw new RuntimeException("User already exists");
        }
        if (emailIsUsed){
            throw new RuntimeException("Email already used");
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
        Boolean emailIsUsed = personRepository.existsByEmail(personRequestDTO.email());

        if (userExists){
            throw new RuntimeException("User already exists");
        }
        if (emailIsUsed){
            throw new RuntimeException("Email already used"); // conflict , fix this
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
                .orElseThrow(() -> new RuntimeException("User doesn't exists for email : " + email));

        return toDTO(person);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Override
    public PersonResponseDTO assignRole(PersonRoleSetDTO roleDTO, String email){
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User doesn't exist for email : " + email));
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
            throw new RuntimeException("User already has role: " + roleName);
        }

        Authority authority = new Authority(
                null,
                roleName,
                person
        );

        person.getAuthorities().add(authority);

        return toDTO(personRepository.save(person));
    }
}
