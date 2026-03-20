package com.pb.catalog.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final Map<String, Object> details;

    public BusinessException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public static BusinessException of(String code, String message) {
        return new BusinessException(code, message, Map.of());
    }

    public static BusinessException of(String code, String message, Map<String, Object> details) {
        return new BusinessException(code, message, details);
    }
}
