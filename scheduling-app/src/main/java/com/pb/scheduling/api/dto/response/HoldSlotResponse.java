package com.pb.scheduling.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class HoldSlotResponse {
    private UUID slotReservationId;
    private OffsetDateTime expiresAt;
}
