package com.pb.auth.api;

import com.pb.auth.api.dto.*;
import com.pb.auth.security.SecurityUtils;
import com.pb.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
