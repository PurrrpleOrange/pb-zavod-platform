package com.pb.catalog.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TariffAddonResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private BigDecimal price;
    private Integer maxQtyPerPlayer;
    private Integer maxQtyPerBooking;
}
