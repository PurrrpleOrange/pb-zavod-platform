package com.pb.catalog.api.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateTariffRequest {

    private String name;

    @PositiveOrZero
    private BigDecimal pricePerPlayer;

    private Boolean active;
}
