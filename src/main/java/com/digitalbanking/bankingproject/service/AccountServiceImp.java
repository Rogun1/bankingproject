package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.constants.AccountStatus;
import com.digitalbanking.bankingproject.constants.AccountType;
import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.AccountResponseDTO;
import com.digitalbanking.bankingproject.dto.AccountsResponseDTO;
import com.digitalbanking.bankingproject.model.Account;
import com.digitalbanking.bankingproject.model.Customer;
import com.digitalbanking.bankingproject.repository.AccountRepository;
import com.digitalbanking.bankingproject.repository.CustomerRepository;
import com.digitalbanking.bankingproject.service.declarations.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccountServiceImp implements AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public AccountServiceImp(AccountRepository accountRepository,
                             CustomerRepository customerRepository){

        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public AccountResponseDTO register(String email, AccountRequestDTO accountRequestDTO) {

        String iban = ibanGenerator(accountRequestDTO.currency());
        Boolean existsByEmail = customerRepository.existsByEmail(email);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer doesn't exist by email: " + email));

        Boolean existsByCurrAndCustomerId = accountRepository.existsByCurrencyAndCustomerId(accountRequestDTO.currency(), customer.getId());

        if (!existsByEmail){
            throw new RuntimeException("Customer doesn't exist by email: " + email);
        }
        if (existsByCurrAndCustomerId){
            throw new RuntimeException
                    ("Account for: " + customer.getEmail() + " already exists with currency: " + accountRequestDTO.currency());
        }

        Date createdAt = new Date();

        Account account = new Account(
                null,
                customer,
                accountRequestDTO.currency(),
                iban,
                0.0,
                AccountType.CURRENT,
                AccountStatus.ACTIVE,
                createdAt
        );

        return toDTO(accountRepository.save(account));
    }

    @Override
    public List<AccountsResponseDTO> accounts(String email){

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer doesn't exist by id: " + email));
        List<AccountsResponseDTO> accounts = accountRepository.findAllByCustomerId(customer.getId()).stream()
                .map(account -> new AccountsResponseDTO(
                        account.getCurrency(),
                        account.getIban(),
                        account.getBalance(),
                        account.getAccountType(),
                        account.getStatus()
                ))
                .toList();

        return accounts;
    }
    //Simulating a IBAN generator
    protected String ibanGenerator(String currency){
        String bankPrefix = "BANK";

        HashMap<String, String> CURRENCY_PREFIX = new HashMap<>();
        CURRENCY_PREFIX.put("Ron", "RO");
        CURRENCY_PREFIX.put("Euro", "EU");
        CURRENCY_PREFIX.put("Pounds", "GB");
        CURRENCY_PREFIX.put("Dollars", "US");

        if (!CURRENCY_PREFIX.values().contains(currency)){
            throw new RuntimeException("Currency doesn't exists: " +
                currency + "\n" +
                "Please select one of existing Currency : " +
                CURRENCY_PREFIX.values());
        }

        Random random = new Random();
        String iban = "";
        Boolean checkIban = true;

        while (checkIban){
            iban = currency + random.nextInt(0, 99) +
                    bankPrefix +
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999);

            Boolean existsIban = accountRepository.existsByIban(iban);

            if (!existsIban){
                checkIban = false;
            }
        }

        return iban;
    }
}
