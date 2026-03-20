package com.pb.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGameTypeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;
}
