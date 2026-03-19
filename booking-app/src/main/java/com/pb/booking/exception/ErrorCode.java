package com.pb.booking.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    BOOKING_NOT_FOUND("Booking not found", HttpStatus.NOT_FOUND),
    BOOKING_INVALID_STATE("Invalid booking state transition", HttpStatus.CONFLICT),
    BOOKING_SLOT_HOLD_FAILED("Failed to hold slot in scheduling service", HttpStatus.UNPROCESSABLE_ENTITY),
    BOOKING_ZONE_HOLD_FAILED("Failed to hold zone in scheduling service", HttpStatus.UNPROCESSABLE_ENTITY),
    BOOKING_CLIENT_INVALID("Client is invalid or not found", HttpStatus.BAD_REQUEST),
    BOOKING_TARIFF_INVALID("Tariff is invalid or not found", HttpStatus.BAD_REQUEST),
    BOOKING_PREPAYMENT_REQUIRED("Prepayment is required for this booking", HttpStatus.UNPROCESSABLE_ENTITY),
    VALIDATION_ERROR("Validation error", HttpStatus.BAD_REQUEST);

    private final String defaultMessage;
    private final HttpStatus httpStatus;
}
