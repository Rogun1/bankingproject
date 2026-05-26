package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.constants.AccountStatus;
import com.digitalbanking.bankingproject.dto.CustomerRequestDTO;
import com.digitalbanking.bankingproject.dto.CustomerResponseDTO;
import com.digitalbanking.bankingproject.model.Authority;
import com.digitalbanking.bankingproject.model.Customer;
import com.digitalbanking.bankingproject.repository.CustomerRepository;
import com.digitalbanking.bankingproject.service.declarations.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomerServiceImp implements CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CustomerServiceImp(CustomerRepository customerRepository, PasswordEncoder passwordEncoder){
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public CustomerResponseDTO register(CustomerRequestDTO customerRequestDTO) {

        customerRepository.findByCNP(customerRequestDTO.CNP())
                .ifPresent( user -> new RuntimeException("User already exists"));

        String hashPwd = passwordEncoder.encode(customerRequestDTO.pwd());

        Customer customer = new Customer(
                null,
                customerRequestDTO.email(),
                hashPwd,
                customerRequestDTO.firstName(),
                customerRequestDTO.lastName(),
                customerRequestDTO.CNP(),
                null,
                AccountStatus.ACTIVE,
                new Date()
        );

        Set<Authority> authorities = Set.of(new Authority(
                null,
                "ROLE_USER",
                customer
        ));

        customer.setAuthorities(authorities);

        return toDTO(customerRepository.save(customer));
    }

    //After connection, it gets the JWT token
    @Override
    public CustomerResponseDTO login(Authentication authentication){
        Optional<Customer> customer = customerRepository.findByEmail(authentication.getName());

        return toDTO(customer.orElse(null));
    }

    @Override
    public CustomerResponseDTO update(String email, CustomerRequestDTO customerRequestDTO){
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account doesn't exist for this email"));
        String hashPwd = passwordEncoder.encode(customerRequestDTO.pwd());

        customer.setFirstName(customerRequestDTO.firstName());
        customer.setLastName(customerRequestDTO.lastName());
        customer.setEmail(customerRequestDTO.email());
        customer.setCNP(customerRequestDTO.CNP());
        customer.setPwd(hashPwd);

        return toDTO(customerRepository.save(customer));
    }
}
