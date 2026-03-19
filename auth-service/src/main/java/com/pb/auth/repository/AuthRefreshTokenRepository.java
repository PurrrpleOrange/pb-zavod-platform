package com.pb.auth.repository;

import com.pb.auth.domain.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, UUID> {

    Optional<AuthRefreshToken> findByJti(String jti);

    @Modifying
    @Query("UPDATE AuthRefreshToken t SET t.revokedAt = CURRENT_TIMESTAMP WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") UUID userId);
}
