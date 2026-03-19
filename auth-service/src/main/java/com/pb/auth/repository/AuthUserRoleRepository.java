package com.pb.auth.repository;

import com.pb.auth.domain.AuthUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRoleRepository extends JpaRepository<AuthUserRole, UUID> {

    Optional<AuthUserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
