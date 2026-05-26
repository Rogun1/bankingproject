package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.model.Customer;
import com.digitalbanking.bankingproject.repository.CustomerRepository;
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

    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User details not found for user: " + username));

        List<GrantedAuthority> authorities = customer.getAuthorities().stream()
                .map(authority -> (GrantedAuthority) new SimpleGrantedAuthority(authority.getName()))
                .toList();

        return new User(customer.getEmail(), customer.getPwd(), authorities);
    }
}
