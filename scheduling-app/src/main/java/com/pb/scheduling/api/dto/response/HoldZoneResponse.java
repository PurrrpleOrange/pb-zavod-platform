package com.pb.scheduling.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class HoldZoneResponse {
    private UUID zoneReservationId;
    private OffsetDateTime expiresAt;
}
