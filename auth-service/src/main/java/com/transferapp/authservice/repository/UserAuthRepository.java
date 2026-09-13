package com.transferapp.authservice.repository;

import com.transferapp.authservice.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthRepository extends JpaRepository<UserAuth, UUID> {

    boolean existsByEmail(String email);

    Optional<UserAuth> findByEmail(String email);
}
