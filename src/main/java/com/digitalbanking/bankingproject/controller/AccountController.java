package com.digitalbanking.bankingproject.controller;

import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.AccountResponseDTO;
import com.digitalbanking.bankingproject.dto.AccountsResponseDTO;
import com.digitalbanking.bankingproject.service.declarations.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public AccountResponseDTO register(Authentication authentication ,@RequestBody AccountRequestDTO accountRequestDTO){
        return accountService.register(authentication.getName() ,accountRequestDTO);
    }

    @GetMapping("/myAccounts")
    public List<AccountsResponseDTO> accounts(Authentication authentication){
        return accountService.accounts(authentication.getName());
    }

//    //Delete for employers only to do
//    @PreAuthorize("hasRole('ROLE_EMPLOYER')")
//    @DeleteMapping
//    public void deleteAccount(){
//
//    }
}
