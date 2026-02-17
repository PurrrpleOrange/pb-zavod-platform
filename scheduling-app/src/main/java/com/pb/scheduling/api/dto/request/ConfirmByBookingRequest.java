package com.pb.scheduling.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ConfirmByBookingRequest {
    @NotNull
    private UUID bookingId;
}
