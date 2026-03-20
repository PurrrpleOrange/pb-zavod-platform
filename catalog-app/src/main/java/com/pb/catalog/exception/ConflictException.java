package com.pb.catalog.exception;

import java.util.Map;

public class ConflictException extends BusinessException {
    public ConflictException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    public static ConflictException of(String code, String message, Map<String, Object> details) {
        return new ConflictException(code, message, details);
    }
}
