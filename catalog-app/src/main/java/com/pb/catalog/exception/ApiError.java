package com.pb.catalog.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ApiError {
    private String code;
    private String message;
    private Map<String, Object> details;
}
