package com.pb.scheduling.api.dto.request;

import com.pb.scheduling.domain.enums.ZoneType;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateZoneRequest {

    private String name;

    private ZoneType type;

    @Positive
    private Integer capacityCompanies;

    private Boolean paid;

    private Boolean active;
}
