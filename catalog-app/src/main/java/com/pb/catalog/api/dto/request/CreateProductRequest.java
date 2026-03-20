package com.pb.catalog.api.dto.request;

import com.pb.catalog.domain.enums.ProductCategory;
import com.pb.catalog.domain.enums.ProductSubcategory;
import com.pb.catalog.domain.enums.ProductUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotNull
    private ProductCategory category;

    private ProductSubcategory subcategory;

    @NotNull
    private ProductUnit unit;

    @NotNull
    @PositiveOrZero
    private BigDecimal currentPrice;
}
