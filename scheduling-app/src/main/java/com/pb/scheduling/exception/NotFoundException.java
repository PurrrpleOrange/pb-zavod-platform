package com.pb.scheduling.exception;

import java.util.Map;

public class NotFoundException extends BusinessException {
    public NotFoundException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }

    public static NotFoundException of(String code, String message, Map<String, Object> details) {
        return new NotFoundException(code, message, details);
    }
}
