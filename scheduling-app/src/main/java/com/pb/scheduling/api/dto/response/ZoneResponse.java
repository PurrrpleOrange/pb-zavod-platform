package com.pb.scheduling.api.dto.response;

import com.pb.scheduling.domain.enums.ZoneType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ZoneResponse {

    private UUID id;
    private String code;
    private String name;
    private ZoneType type;
    private int capacityCompanies;
    private boolean paid;
    private boolean active;
}
