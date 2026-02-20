package com.pb.scheduling.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoldZoneRequest {
    @NotNull
    private UUID bookingId;

    @NotNull
    private OffsetDateTime startTime;

    @NotNull
    private OffsetDateTime endTime;

    @NotNull
    private Integer holdMinutes = 15;

    HoldZoneRequest(UUID bookingId, OffsetDateTime startTime, OffsetDateTime endTime) {
        this.bookingId = bookingId;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
