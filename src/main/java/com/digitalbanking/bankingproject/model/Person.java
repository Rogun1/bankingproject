package com.digitalbanking.bankingproject.model;

import com.digitalbanking.bankingproject.constants.PersonAccountStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "person")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String pwd;
    private String firstName;
    private String lastName;
    private Long CNP;
    @OneToMany(mappedBy = "person", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<Authority> authorities;
    private PersonAccountStatus status = PersonAccountStatus.ONHOLD;
    private Date createdAt;
}
