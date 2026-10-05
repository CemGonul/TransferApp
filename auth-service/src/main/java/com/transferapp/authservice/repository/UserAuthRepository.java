package com.transferapp.authservice.repository;

import com.transferapp.authservice.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthRepository extends JpaRepository<UserAuth, UUID> {

    boolean existsByEmail(String email);

    Optional<UserAuth> findByEmail(String email);

    @Transactional
    @Modifying
    @Query("UPDATE UserAuth user SET user.registrationCompleted = true WHERE user.id = :userId")
    int completeRegistration(@Param("userId") UUID userId);
}
