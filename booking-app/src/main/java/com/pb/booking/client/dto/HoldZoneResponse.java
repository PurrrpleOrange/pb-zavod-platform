package com.pb.booking.client.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class HoldZoneResponse {

    private UUID zoneReservationId;
    private OffsetDateTime expiresAt;
}
