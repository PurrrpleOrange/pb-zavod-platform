package com.pb.auth.service;

import com.pb.auth.api.dto.CreateUserRequest;
import com.pb.auth.api.dto.UpdateUserRequest;
import com.pb.auth.api.dto.UserResponse;
import com.pb.auth.audit.AuditService;
import com.pb.auth.domain.*;
import com.pb.auth.exception.AuthException;
import com.pb.auth.exception.ErrorCode;
import com.pb.auth.mapper.UserMapper;
import com.pb.auth.repository.AuthRoleRepository;
import com.pb.auth.repository.AuthUserRepository;
import com.pb.auth.repository.AuthUserRoleRepository;
import com.pb.auth.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthUserRepository userRepository;
    private final AuthRoleRepository roleRepository;
    private final AuthUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new AuthException(ErrorCode.AUTH_LOGIN_EXISTS);
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException(ErrorCode.AUTH_EMAIL_EXISTS);
        }

        UUID actorId = SecurityUtils.getCurrentUserId().orElse(null);

        AuthUser user = AuthUser.builder()
                .login(request.getLogin().trim().toLowerCase())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        user = userRepository.save(user);

        if (request.getRoles() != null) {
            for (String roleCode : request.getRoles()) {
                assignRoleInternal(user, roleCode, actorId);
            }
        }

        AuthUser loaded = userRepository.findByIdWithRoles(user.getId())
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

        auditService.logSuccess(EventType.USER_CREATED, actorId, loaded.getId());
        return userMapper.toResponse(loaded);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(UserStatus status, String search, Pageable pageable) {
        return userRepository.findAllFiltered(status, search, pageable)
                .map(user -> {
                    AuthUser loaded = userRepository.findByIdWithRoles(user.getId())
                            .orElse(user);
                    return userMapper.toResponse(loaded);
                });
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        AuthUser user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        AuthUser user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

        UUID actorId = SecurityUtils.getCurrentUserId().orElse(null);

        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new AuthException(ErrorCode.AUTH_EMAIL_EXISTS);
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getStatus() != null) {
            UserStatus newStatus = UserStatus.valueOf(request.getStatus());

            if (newStatus == UserStatus.DISABLED && user.getStatus() == UserStatus.ACTIVE) {
                checkNotLastAdmin(user);
            }

            user.setStatus(newStatus);

            if (newStatus == UserStatus.DISABLED) {
                auditService.logSuccess(EventType.USER_DISABLED, actorId, user.getId());
            }
        }

        user.setUpdatedBy(actorId);
        userRepository.save(user);

        auditService.logSuccess(EventType.USER_UPDATED, actorId, user.getId());
        return userMapper.toResponse(user);
    }

    @Transactional
    public void assignRole(UUID userId, String roleCode) {
        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

        UUID actorId = SecurityUtils.getCurrentUserId().orElse(null);
        assignRoleInternal(user, roleCode, actorId);
        auditService.logSuccess(EventType.ROLE_GRANTED, actorId, userId);
    }

    @Transactional
    public void removeRole(UUID userId, String roleCode) {
        AuthUser user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

        AuthRole role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_ROLE_NOT_FOUND));

        if (!userRoleRepository.existsByUserIdAndRoleId(userId, role.getId())) {
            throw new AuthException(ErrorCode.AUTH_ROLE_NOT_FOUND);
        }

        // Check: cannot remove the last active ADMIN
        if ("ADMIN".equals(roleCode) && user.getStatus() == UserStatus.ACTIVE) {
            long adminCount = userRepository.countActiveAdmins();
            if (adminCount <= 1) {
                throw new AuthException(ErrorCode.AUTH_LAST_ADMIN);
            }
        }

        UUID actorId = SecurityUtils.getCurrentUserId().orElse(null);
        userRoleRepository.deleteByUserIdAndRoleId(userId, role.getId());
        auditService.logSuccess(EventType.ROLE_REVOKED, actorId, userId);
    }

    private void assignRoleInternal(AuthUser user, String roleCode, UUID actorId) {
        AuthRole role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_ROLE_NOT_FOUND));

        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            throw new AuthException(ErrorCode.AUTH_ROLE_ALREADY_ASSIGNED);
        }

        AuthUserRole userRole = AuthUserRole.builder()
                .user(user)
                .role(role)
                .assignedBy(actorId)
                .build();

        userRoleRepository.save(userRole);
    }

    private void checkNotLastAdmin(AuthUser user) {
        boolean isAdmin = user.getUserRoles().stream()
                .anyMatch(ur -> "ADMIN".equals(ur.getRole().getCode()));

        if (isAdmin) {
            long adminCount = userRepository.countActiveAdmins();
            if (adminCount <= 1) {
                throw new AuthException(ErrorCode.AUTH_LAST_ADMIN);
            }
        }
    }
}
