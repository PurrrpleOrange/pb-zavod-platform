package com.pb.catalog.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AddTariffAddonRequest {

    @NotNull
    private UUID productId;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    @PositiveOrZero
    private Integer maxQtyPerPlayer;

    @PositiveOrZero
    private Integer maxQtyPerBooking;
}
