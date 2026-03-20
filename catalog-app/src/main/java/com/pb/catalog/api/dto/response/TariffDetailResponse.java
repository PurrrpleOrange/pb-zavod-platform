package com.pb.catalog.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TariffDetailResponse {
    private UUID id;
    private UUID gameTypeId;
    private String name;
    private BigDecimal pricePerPlayer;
    private boolean active;
    private List<TariffIncludedItemResponse> includedItems;
    private List<TariffAddonResponse> addons;
}
