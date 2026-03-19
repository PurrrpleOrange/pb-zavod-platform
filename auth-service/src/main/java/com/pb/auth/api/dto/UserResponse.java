package com.pb.auth.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String login;
    private String email;
    private String phone;
    private String status;
    private List<String> roles;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
