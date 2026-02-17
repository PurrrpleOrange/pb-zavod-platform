package com.pb.scheduling.api.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ExtendZoneRequest {
    // либо newEndTime, либо extendMinutes
    private OffsetDateTime newEndTime;

    @Positive
    private Integer extendMinutes;
}
