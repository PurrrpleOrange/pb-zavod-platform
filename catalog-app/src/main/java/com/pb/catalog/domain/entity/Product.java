package com.pb.catalog.domain.entity;

import com.pb.catalog.domain.enums.ProductCategory;
import com.pb.catalog.domain.enums.ProductSubcategory;
import com.pb.catalog.domain.enums.ProductUnit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "subcategory")
    private ProductSubcategory subcategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private ProductUnit unit;

    @Column(name = "current_price", nullable = false)
    private BigDecimal currentPrice;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
