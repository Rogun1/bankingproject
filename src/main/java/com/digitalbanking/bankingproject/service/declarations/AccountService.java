package com.digitalbanking.bankingproject.service.declarations;

import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.AccountResponseDTO;
import com.digitalbanking.bankingproject.dto.AccountsResponseDTO;
import com.digitalbanking.bankingproject.model.Account;

import java.util.List;

public interface AccountService {
    AccountResponseDTO register(String email, AccountRequestDTO accountRequestDTO);
    List<AccountsResponseDTO> accounts(String email);

    default AccountResponseDTO toDTO(Account account){
        return new AccountResponseDTO(
                account.getCurrency(),
                account.getIban(),
                account.getBalance(),
                account.getAccountType(),
                account.getStatus()
        );
    }
}
