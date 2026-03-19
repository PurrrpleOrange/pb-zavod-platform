package com.pb.auth.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String login;
    private List<String> roles;
}
