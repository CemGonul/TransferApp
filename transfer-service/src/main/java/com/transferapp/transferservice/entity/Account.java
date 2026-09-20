package com.transferapp.transferservice.entity;


import com.transferapp.transferservice.enums.Currency;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(
        name="accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name="uk_account_number",
                        columnNames = "account_number"
                ),
                @UniqueConstraint(
                        name="uk_user_currency",
                        columnNames = {"user_id","currency"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name="user_id",nullable = false)
    private UUID userId;

    @Column(name="account_number",nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal balance;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Account(
            UUID userId,
            String accountNumber,
            Currency currency

    ){
        this.userId=userId;
        this.accountNumber=accountNumber;
        this.currency=currency;
        this.balance=BigDecimal.ZERO;
    }


}
