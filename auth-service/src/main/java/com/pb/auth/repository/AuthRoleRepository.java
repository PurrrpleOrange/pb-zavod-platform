package com.pb.auth.repository;

import com.pb.auth.domain.AuthRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthRoleRepository extends JpaRepository<AuthRole, UUID> {

    Optional<AuthRole> findByCode(String code);

    boolean existsByCode(String code);
}
