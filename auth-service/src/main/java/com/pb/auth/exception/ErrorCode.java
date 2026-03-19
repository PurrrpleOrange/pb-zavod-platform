package com.pb.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    AUTH_INVALID_CREDENTIALS("Invalid login or password", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_DISABLED("Account is disabled", HttpStatus.FORBIDDEN),
    AUTH_ACCOUNT_LOCKED("Account is temporarily locked", HttpStatus.FORBIDDEN),
    AUTH_TOKEN_EXPIRED("Token has expired", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("Token is invalid", HttpStatus.UNAUTHORIZED),
    AUTH_REFRESH_REUSED("Refresh token reuse detected", HttpStatus.UNAUTHORIZED),
    AUTH_ROLE_NOT_FOUND("Role not found", HttpStatus.NOT_FOUND),
    AUTH_USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    AUTH_PERMISSION_DENIED("Permission denied", HttpStatus.FORBIDDEN),
    AUTH_LAST_ADMIN("Cannot remove the last active admin", HttpStatus.CONFLICT),
    AUTH_LOGIN_EXISTS("Login already exists", HttpStatus.CONFLICT),
    AUTH_EMAIL_EXISTS("Email already exists", HttpStatus.CONFLICT),
    AUTH_ROLE_ALREADY_ASSIGNED("Role already assigned", HttpStatus.CONFLICT),
    VALIDATION_ERROR("Validation error", HttpStatus.BAD_REQUEST);

    private final String defaultMessage;
    private final HttpStatus httpStatus;
}
