package com.pb.catalog.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TariffIncludedItemResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private Integer quantityPerPlayer;
    private Integer quantityFixed;
}
