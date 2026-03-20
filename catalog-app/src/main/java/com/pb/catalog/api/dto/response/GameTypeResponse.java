package com.pb.catalog.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class GameTypeResponse {
    private UUID id;
    private String code;
    private String name;
}
