package com.digitalbanking.bankingproject.repository;

import com.digitalbanking.bankingproject.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person,Long> {

    Optional<Person> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Person> findByCNP(Long CNP);
    boolean existsByCNP(Long CNP);
}
