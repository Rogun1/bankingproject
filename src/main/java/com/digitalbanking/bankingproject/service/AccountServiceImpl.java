package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.constants.AccountStatus;
import com.digitalbanking.bankingproject.constants.AccountType;
import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.AccountResponseDTO;
import com.digitalbanking.bankingproject.dto.AccountsResponseDTO;
import com.digitalbanking.bankingproject.exceptions.AlreadyExistsException;
import com.digitalbanking.bankingproject.exceptions.BalanceZeroException;
import com.digitalbanking.bankingproject.exceptions.NotFoundException;
import com.digitalbanking.bankingproject.model.Account;
import com.digitalbanking.bankingproject.model.Card;
import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.repository.AccountRepository;
import com.digitalbanking.bankingproject.repository.CardRepository;
import com.digitalbanking.bankingproject.repository.PersonRepository;
import com.digitalbanking.bankingproject.service.declarations.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final CardRepository cardRepository;

    @Autowired
    public AccountServiceImpl(AccountRepository accountRepository,
                              PersonRepository personRepository,
                              CardRepository cardRepository){

        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
        this.cardRepository = cardRepository;
    }

    @Override
    public AccountResponseDTO register(String email, AccountRequestDTO accountRequestDTO) {

        String iban = ibanGenerator(accountRequestDTO.currency());
        boolean existsByEmail = personRepository.existsByEmail(email);
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User doesn't exist by email: " + email));

        boolean existsByCurrAndPersonId = accountRepository.existsByCurrencyAndPersonId(accountRequestDTO.currency(), person.getId());

        if (!existsByEmail){
            throw new NotFoundException("User doesn't exist by email: " + email);
        }
        if (existsByCurrAndPersonId){
            throw new AlreadyExistsException
                    ("Account for: " + person.getEmail() + " already exists with currency: " + accountRequestDTO.currency());
        }

        Date createdAt = new Date();

        Account account = new Account(
                null,
                person,
                accountRequestDTO.currency(),
                iban,
                new BigDecimal(BigInteger.ZERO),
                AccountType.CURRENT,
                AccountStatus.ACTIVE,
                createdAt
        );

        return toDTO(accountRepository.save(account));
    }

    @Override
    public List<AccountsResponseDTO> accounts(String email){

        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User doesn't exist by id: " + email));
        List<AccountsResponseDTO> accounts = accountRepository.findAllByPersonId(person.getId()).stream()
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

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYER')")
    @Override
    public void delete(Long accountId){
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found for id: " + accountId));
        boolean existsCard = cardRepository.existsCardByAccountId(account.getId());

        //Check this
        if (existsCard){
            // It will never throw, because we know it exists from boolean
            // But it needs throw from repository
            Card card = cardRepository.findByAccountId(account.getId())
                    .orElseThrow(() -> new NotFoundException("Card not found "));
            cardRepository.delete(card);
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0){
            throw new BalanceZeroException("Account balance must be 0 to be deleted, balance: " + account.getBalance());
        }

        accountRepository.delete(account);
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
            throw new NotFoundException("Currency doesn't exists: " +
                currency + "\n" +
                "Please select one of existing Currency : " +
                CURRENCY_PREFIX.values());
        }

        Random random = new Random();
        String iban = "";
        boolean checkIban = true;

        while (checkIban){
            iban = currency + random.nextInt(0, 99) +
                    bankPrefix +
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999);

            boolean existsIban = accountRepository.existsByIban(iban);

            if (!existsIban){
                checkIban = false;
            }
        }

        return iban;
    }
}
