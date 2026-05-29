package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankProjectUserDetailsService implements UserDetailsService {

    private final PersonRepository personRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User details not found for email: " + email));

        List<GrantedAuthority> authorities = person.getAuthorities().stream()
                .map(authority -> (GrantedAuthority) new SimpleGrantedAuthority(authority.getName()))
                .toList();

        return new User(person.getEmail(), person.getPwd(), authorities);
    }
}
