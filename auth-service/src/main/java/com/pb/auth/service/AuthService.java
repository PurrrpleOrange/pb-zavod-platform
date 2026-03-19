package com.pb.auth.service;

import com.pb.auth.api.dto.LoginRequest;
import com.pb.auth.api.dto.TokenResponse;
import com.pb.auth.audit.AuditService;
import com.pb.auth.config.AuthProperties;
import com.pb.auth.domain.*;
import com.pb.auth.exception.AuthException;
import com.pb.auth.exception.ErrorCode;
import com.pb.auth.repository.AuthRefreshTokenRepository;
import com.pb.auth.repository.AuthUserRepository;
import com.pb.auth.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository userRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final AuditService auditService;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        AuthUser user = userRepository.findByLoginWithRoles(request.getLogin())
                .orElseThrow(() -> {
                    auditService.logFailure(EventType.LOGIN_FAILED, null, null, ErrorCode.AUTH_INVALID_CREDENTIALS.name());
                    return new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS);
                });

        if (user.getStatus() == UserStatus.DISABLED) {
            auditService.logFailure(EventType.LOGIN_FAILED, null, user.getId(), ErrorCode.AUTH_ACCOUNT_DISABLED.name());
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            auditService.logFailure(EventType.LOGIN_FAILED, null, user.getId(), ErrorCode.AUTH_ACCOUNT_LOCKED.name());
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);
            auditService.logFailure(EventType.LOGIN_FAILED, null, user.getId(), ErrorCode.AUTH_INVALID_CREDENTIALS.name());
            throw new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roles = extractRoles(user);
        TokenResponse tokenResponse = generateTokens(user, roles);

        auditService.logSuccess(EventType.LOGIN_SUCCESS, user.getId(), user.getId());
        return tokenResponse;
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        AuthRefreshToken storedToken = refreshTokenRepository.findByJti(extractJtiFromRefreshToken(rawRefreshToken))
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_TOKEN_INVALID));

        if (!storedToken.getTokenHash().equals(tokenHash)) {
            throw new AuthException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        if (storedToken.isRevoked()) {
            // Reuse detection — revoke all tokens for this user
            refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            auditService.logFailure(EventType.REFRESH_REUSED, storedToken.getUser().getId(), storedToken.getUser().getId(), ErrorCode.AUTH_REFRESH_REUSED.name());
            throw new AuthException(ErrorCode.AUTH_REFRESH_REUSED);
        }

        if (storedToken.isExpired()) {
            throw new AuthException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }

        AuthUser user = userRepository.findByIdWithRoles(storedToken.getUser().getId())
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        // Rotate: revoke old, issue new
        List<String> roles = extractRoles(user);
        String newRefreshTokenRaw = UUID.randomUUID().toString();
        String newJti = UUID.randomUUID().toString();

        storedToken.setRevokedAt(Instant.now());
        storedToken.setReplacedByJti(newJti);
        refreshTokenRepository.save(storedToken);

        AuthRefreshToken newToken = AuthRefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(newRefreshTokenRaw))
                .jti(newJti)
                .expiresAt(Instant.now().plus(authProperties.getRefresh().getTtl()))
                .build();
        refreshTokenRepository.save(newToken);

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getLogin(), roles);

        auditService.logSuccess(EventType.REFRESH_SUCCESS, user.getId(), user.getId());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newJti + ":" + newRefreshTokenRaw)
                .userId(user.getId())
                .login(user.getLogin())
                .roles(roles)
                .build();
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        auditService.logSuccess(EventType.LOGOUT, userId, userId);
    }

    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_USER_NOT_FOUND));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens on password change
        refreshTokenRepository.revokeAllByUserId(userId);

        auditService.logSuccess(EventType.PASSWORD_CHANGED, userId, userId);
    }

    private TokenResponse generateTokens(AuthUser user, List<String> roles) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getLogin(), roles);

        String refreshTokenRaw = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        AuthRefreshToken refreshToken = AuthRefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshTokenRaw))
                .jti(jti)
                .expiresAt(Instant.now().plus(authProperties.getRefresh().getTtl()))
                .build();

        refreshTokenRepository.save(refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(jti + ":" + refreshTokenRaw)
                .userId(user.getId())
                .login(user.getLogin())
                .roles(roles)
                .build();
    }

    private List<String> extractRoles(AuthUser user) {
        return user.getUserRoles().stream()
                .map(ur -> ur.getRole().getCode())
                .toList();
    }

    private String extractJtiFromRefreshToken(String rawRefreshToken) {
        int idx = rawRefreshToken.indexOf(':');
        if (idx < 0) {
            throw new AuthException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        return rawRefreshToken.substring(0, idx);
    }

    String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
