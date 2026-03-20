package com.pb.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "tariff_addon")
public class TariffAddon {

    @Id
    @Column(name = "tariff_addon_id", nullable = false)
    private UUID id;

    @Column(name = "tariff_id", nullable = false)
    private UUID tariffId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "max_qty_per_player")
    private Integer maxQtyPerPlayer;

    @Column(name = "max_qty_per_booking")
    private Integer maxQtyPerBooking;
}
