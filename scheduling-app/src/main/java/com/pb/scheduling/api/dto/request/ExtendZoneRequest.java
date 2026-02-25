package com.pb.scheduling.api.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ExtendZoneRequest {
    private UUID bookingId;
    private Integer extendMinutes;
}

