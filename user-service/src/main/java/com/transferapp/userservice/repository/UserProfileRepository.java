package com.transferapp.userservice.repository;

import com.transferapp.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO user_profile (user_id, status, created_at, updated_at)
            VALUES (:userId, 'INCOMPLETE', :now, :now)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("now") Instant now);
}
