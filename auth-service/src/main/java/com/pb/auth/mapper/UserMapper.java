package com.pb.auth.mapper;

import com.pb.auth.api.dto.UserResponse;
import com.pb.auth.domain.AuthUser;
import com.pb.auth.domain.AuthUserRole;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserResponse toResponse(AuthUser user) {
        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getCode())
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
