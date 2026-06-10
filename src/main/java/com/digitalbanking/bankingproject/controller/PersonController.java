package com.digitalbanking.bankingproject.controller;

import com.digitalbanking.bankingproject.dto.PersonRequestDTO;
import com.digitalbanking.bankingproject.dto.PersonResponseDTO;
import com.digitalbanking.bankingproject.dto.PersonRoleSetDTO;
import com.digitalbanking.bankingproject.service.declarations.PersonService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping("/register")
    public PersonResponseDTO register(@RequestBody @Valid PersonRequestDTO personRequestDTO){
        return personService.register(personRequestDTO);
    }

    @GetMapping("/login")
    public PersonResponseDTO login(Authentication authentication){
        return personService.login(authentication);
    }

    @PutMapping("/update")
    public PersonResponseDTO update(Authentication authentication, @RequestBody @Valid PersonRequestDTO personRequestDTO){
        return personService.update(authentication.getName(), personRequestDTO);
    }

    @GetMapping("/me")
    public PersonResponseDTO profile(Authentication authentication){
        return personService.profile(authentication.getName());
    }

    @PatchMapping("/{email}/roles")
    public PersonResponseDTO assignRole(@RequestBody @Valid PersonRoleSetDTO role, @PathVariable @Email String email){
        return personService.assignRole(role, email);
    }

    @DeleteMapping("/{personId}/delete")
    public void delete(@PathVariable @NotNull Long personId){
        personService.delete(personId);
    }
}
