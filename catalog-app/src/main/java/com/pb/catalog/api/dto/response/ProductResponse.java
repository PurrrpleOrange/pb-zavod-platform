package com.pb.catalog.api.dto.response;

import com.pb.catalog.domain.enums.ProductCategory;
import com.pb.catalog.domain.enums.ProductSubcategory;
import com.pb.catalog.domain.enums.ProductUnit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {
    private UUID id;
    private String code;
    private String name;
    private ProductCategory category;
    private ProductSubcategory subcategory;
    private ProductUnit unit;
    private BigDecimal currentPrice;
    private boolean active;
}
