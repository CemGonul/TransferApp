package com.transferapp.userservice.entity;


import com.transferapp.userservice.enums.ProfileStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {
    @Id
    @Column(name="user_id",nullable = false,updatable = false)
    private UUID userId;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 32)
    private String phoneNumber;

    @Column(length = 2)
    private String countryCode;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileStatus status;

    @Column(nullable = false,updatable = false)
    private Instant createdAt;

    @Column(nullable=false)
    private Instant updatedAt;

    public UserProfile(UUID userId) {
        this.userId = userId;
        this.status = ProfileStatus.INCOMPLETE;

        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updatePersonalInformation(
            String firstName,
            String lastName,
            String phoneNumber,
            String countryCode) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.countryCode = countryCode;
        this.status = ProfileStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }
}
