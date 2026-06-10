package com.digitalbanking.bankingproject.controller;

import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.AccountResponseDTO;
import com.digitalbanking.bankingproject.dto.AccountsResponseDTO;
import com.digitalbanking.bankingproject.service.declarations.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    public AccountResponseDTO register(Authentication authentication ,@RequestBody @Valid AccountRequestDTO accountRequestDTO){
        return accountService.register(authentication.getName() ,accountRequestDTO);
    }

    @GetMapping("/myAccounts")
    public List<AccountsResponseDTO> accounts(Authentication authentication){
        return accountService.accounts(authentication.getName());
    }

    @DeleteMapping("/{accountId}/delete")
    public void deleteAccount(@PathVariable @NotNull Long accountId){
        accountService.delete(accountId);
    }
}
