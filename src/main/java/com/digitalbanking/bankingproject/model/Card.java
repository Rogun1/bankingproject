package com.digitalbanking.bankingproject.model;

import com.digitalbanking.bankingproject.constants.CardStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "cards")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    private String cardNumber;
    private Integer cvv;
    private LocalDate expirationDate;
    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.ONHOLD;
    private Integer dailyLimit;
    private Date createdAt;
}
