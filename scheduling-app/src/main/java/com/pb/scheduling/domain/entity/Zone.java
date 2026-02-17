package com.pb.scheduling.domain.entity;

import com.pb.scheduling.domain.enums.ZoneType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "zone")
public class Zone {

    @Id
    @Column(name = "zone_id", nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ZoneType type;

    @Column(name = "capacity_companies", nullable = false)
    private int capacityCompanies;

    @Column(name = "is_paid", nullable = false)
    private boolean paid = true;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
