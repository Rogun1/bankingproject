package com.digitalbanking.bankingproject.repository;

import com.digitalbanking.bankingproject.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Boolean existsByCurrencyAndCustomerId(String currency, Long customerId);
    Optional<Account> findByIban(String iban);
    Boolean existsByIban(String iban);
    List<Account> findAllByCustomerId(Long customerId);
}
