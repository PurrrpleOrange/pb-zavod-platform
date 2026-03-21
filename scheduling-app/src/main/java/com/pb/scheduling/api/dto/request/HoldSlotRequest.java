package com.pb.scheduling.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class HoldSlotRequest {
    @NotNull
    private UUID bookingId;

    private boolean exclusive;
}
