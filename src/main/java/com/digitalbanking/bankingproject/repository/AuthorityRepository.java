package com.digitalbanking.bankingproject.repository;

import com.digitalbanking.bankingproject.model.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority,Long> {

    Set<Authority> findAllByPersonId(Long personId);
}
