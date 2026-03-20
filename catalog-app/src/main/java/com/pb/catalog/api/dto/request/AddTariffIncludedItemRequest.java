package com.pb.catalog.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.UUID;

@Data
public class AddTariffIncludedItemRequest {

    @NotNull
    private UUID productId;

    @PositiveOrZero
    private Integer quantityPerPlayer;

    @PositiveOrZero
    private Integer quantityFixed;
}
