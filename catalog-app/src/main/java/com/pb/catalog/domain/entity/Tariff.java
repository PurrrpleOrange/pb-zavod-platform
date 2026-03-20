package com.pb.catalog.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "tariff")
public class Tariff {

    @Id
    @Column(name = "tariff_id", nullable = false)
    private UUID id;

    @Column(name = "game_type_id", nullable = false)
    private UUID gameTypeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price_per_player", nullable = false)
    private BigDecimal pricePerPlayer;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
