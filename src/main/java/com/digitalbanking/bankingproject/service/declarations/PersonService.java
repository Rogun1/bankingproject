package com.digitalbanking.bankingproject.service.declarations;

import com.digitalbanking.bankingproject.dto.PersonRequestDTO;
import com.digitalbanking.bankingproject.dto.PersonResponseDTO;
import com.digitalbanking.bankingproject.dto.PersonRoleSetDTO;
import com.digitalbanking.bankingproject.model.Person;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface PersonService {

    PersonResponseDTO register(PersonRequestDTO personRequestDTO);
    PersonResponseDTO login(Authentication authentication);
    PersonResponseDTO update(String email, PersonRequestDTO personRequestDTO);
    PersonResponseDTO profile(String email);
    PersonResponseDTO assignRole(PersonRoleSetDTO role, String email);

    default PersonResponseDTO toDTO(Person person){
        List<String> authorities = person.getAuthorities().stream()
                .map((authority -> authority.getName()))
                .toList();

        PersonResponseDTO personResponseDTO = new PersonResponseDTO(
                person.getFirstName() + " " + person.getLastName(),
                person.getEmail(),
                authorities,
                person.getStatus().toString()
        );
        return personResponseDTO;
    }
}
