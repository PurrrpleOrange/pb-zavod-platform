package com.pb.scheduling.api.advice;

import com.pb.scheduling.exception.ApiError;
import com.pb.scheduling.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled errorId={}", errorId, ex); // <-- stacktrace появится

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        "INTERNAL_ERROR",
                        "Unexpected error",
                        Map.of("errorId", errorId)
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        // BUG-5: NotFoundException → 404, BusinessException → 422 (не 400)
        HttpStatus status = ex instanceof com.pb.scheduling.exception.NotFoundException
                ? HttpStatus.NOT_FOUND
                : HttpStatus.UNPROCESSABLE_ENTITY;

        log.warn("Business error [{}]: {} details={}", ex.getCode(), ex.getMessage(), ex.getDetails());

        return ResponseEntity.status(status)
                .body(new ApiError(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalid(MethodArgumentNotValidException ex) {
        Map<String, Object> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a
                ));
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Validation failed", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Validation failed", Map.of("error", ex.getMessage())));
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiError> handleOther(Exception ex) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(new ApiError("INTERNAL_ERROR", "Unexpected error", Map.of()));
//    }
}
