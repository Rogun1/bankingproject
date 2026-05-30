package com.digitalbanking.bankingproject.repository;

import com.digitalbanking.bankingproject.model.TransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit,Long> {

    Optional<TransactionLimit> findByPersonId(Long personId);
}
