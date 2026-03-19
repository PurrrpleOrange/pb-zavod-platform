package com.pb.scheduling.api.dto.response;

import com.pb.scheduling.domain.enums.ZoneReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ZoneReservationResponse {

    private UUID id;
    private UUID zoneId;
    private UUID bookingId;
    private UUID slotReservationId;
    private UUID parentReservationId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private ZoneReservationStatus status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
