package com.pb.auth.repository;

import com.pb.auth.domain.AuthUser;
import com.pb.auth.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {

    Optional<AuthUser> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM AuthUser u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.login = :login")
    Optional<AuthUser> findByLoginWithRoles(@Param("login") String login);

    @Query("SELECT u FROM AuthUser u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.id = :id")
    Optional<AuthUser> findByIdWithRoles(@Param("id") UUID id);

    @Query("""
        SELECT u FROM AuthUser u
        WHERE (:status IS NULL OR u.status = :status)
        AND (:search IS NULL OR LOWER(u.login) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<AuthUser> findAllFiltered(
            @Param("status") UserStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT COUNT(ur) FROM AuthUserRole ur JOIN ur.role r JOIN ur.user u WHERE r.code = 'ADMIN' AND u.status = 'ACTIVE'")
    long countActiveAdmins();
}
