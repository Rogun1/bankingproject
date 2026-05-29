package com.digitalbanking.bankingproject.repository;

import com.digitalbanking.bankingproject.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Boolean existsByCurrencyAndPersonId(String currency, Long personId);
    Optional<Account> findByIban(String iban);
    Boolean existsByIban(String iban);
    List<Account> findAllByPersonId(Long personId);
    Optional<Account> findByPersonId(Long personId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForTransaction(@Param("id") Long personId);
}
