package com.pb.scheduling.api.dto.request;

import com.pb.scheduling.domain.enums.ZoneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateZoneRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotNull
    private ZoneType type;

    @NotNull
    @Positive
    private Integer capacityCompanies;

    private boolean paid = true;
}
