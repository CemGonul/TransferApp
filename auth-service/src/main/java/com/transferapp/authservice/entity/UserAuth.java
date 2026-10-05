package com.transferapp.authservice.entity;

import com.transferapp.authservice.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.Instant;
import java.util.UUID;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 255)
    private String hashedPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean registrationCompleted;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;


    public UserAuth(String email, String hashedPassword) {
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.role = Role.USER;
        this.enabled = true;
        this.createdAt = Instant.now();
    }
}
