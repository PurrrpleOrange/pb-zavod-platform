package com.pb.catalog.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateTariffRequest {

    @NotNull
    private UUID gameTypeId;

    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private BigDecimal pricePerPlayer;
}
