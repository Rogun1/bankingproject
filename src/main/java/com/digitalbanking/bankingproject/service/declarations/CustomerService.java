package com.digitalbanking.bankingproject.service.declarations;

import com.digitalbanking.bankingproject.dto.CustomerRequestDTO;
import com.digitalbanking.bankingproject.dto.CustomerResponseDTO;
import com.digitalbanking.bankingproject.model.Customer;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CustomerService {

    CustomerResponseDTO register(CustomerRequestDTO customerRequestDTO);
    CustomerResponseDTO login(Authentication authentication);
    CustomerResponseDTO update(String email, CustomerRequestDTO customerRequestDTO);

    default CustomerResponseDTO toDTO(Customer customer){
        List<String> authorities = customer.getAuthorities().stream()
                .map((authority -> authority.getName()))
                .toList();

        CustomerResponseDTO customerResponseDTO = new CustomerResponseDTO(
                customer.getFirstName() + " " + customer.getLastName(),
                customer.getEmail(),
                authorities,
                customer.getStatus().toString()
        );
        return customerResponseDTO;
    }
}
