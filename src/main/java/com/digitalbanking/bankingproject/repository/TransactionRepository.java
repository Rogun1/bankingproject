package com.digitalbanking.bankingproject.repository;

import com.digitalbanking.bankingproject.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {

    List<Transaction> findAllByFromAccountIdAndCreatedAtDate(Long accountId,LocalDate date);
    List<Transaction> findAllByFromAccountIdAndCreatedAtDateBetween(Long accountId,LocalDate start, LocalDate end);

}
