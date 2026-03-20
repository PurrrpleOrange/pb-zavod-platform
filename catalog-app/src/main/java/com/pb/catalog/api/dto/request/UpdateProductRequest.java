package com.pb.catalog.api.dto.request;

import com.pb.catalog.domain.enums.ProductCategory;
import com.pb.catalog.domain.enums.ProductSubcategory;
import com.pb.catalog.domain.enums.ProductUnit;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {

    private String name;

    private ProductCategory category;

    private ProductSubcategory subcategory;

    private ProductUnit unit;

    @PositiveOrZero
    private BigDecimal currentPrice;

    private Boolean active;
}
