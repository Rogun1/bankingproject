package com.digitalbanking.bankingproject.controller;

import com.digitalbanking.bankingproject.dto.CustomerRequestDTO;
import com.digitalbanking.bankingproject.dto.CustomerResponseDTO;
import com.digitalbanking.bankingproject.service.declarations.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/register")
    public CustomerResponseDTO register(@RequestBody CustomerRequestDTO CustomerRequestDTO){
        return customerService.register(CustomerRequestDTO);
    }

    @GetMapping("/user")
    public CustomerResponseDTO login(Authentication authentication){
        return customerService.login(authentication);
    }

    @PutMapping("/update/{email}")
    public CustomerResponseDTO update(@PathVariable String email, @RequestBody CustomerRequestDTO customerRequestDTO){
        return customerService.update(email, customerRequestDTO);
    }

}
