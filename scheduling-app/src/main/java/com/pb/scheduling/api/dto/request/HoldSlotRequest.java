package com.pb.scheduling.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class HoldSlotRequest {
    @NotNull
    private UUID bookingId;
}
