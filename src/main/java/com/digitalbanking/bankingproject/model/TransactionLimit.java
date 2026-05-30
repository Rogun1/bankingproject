package com.digitalbanking.bankingproject.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transaction_limits")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "person_id")
    private Person person;
    private Double dailyLimit;
    private Double perTransactionLimit;
    private Integer maxTransactionsLimitDaily;

}
