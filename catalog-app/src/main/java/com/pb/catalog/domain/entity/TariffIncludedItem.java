package com.pb.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "tariff_included_item")
public class TariffIncludedItem {

    @Id
    @Column(name = "tariff_included_item_id", nullable = false)
    private UUID id;

    @Column(name = "tariff_id", nullable = false)
    private UUID tariffId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity_per_player")
    private Integer quantityPerPlayer;

    @Column(name = "quantity_fixed")
    private Integer quantityFixed;
}
