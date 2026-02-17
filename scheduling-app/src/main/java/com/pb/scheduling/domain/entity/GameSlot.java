package com.pb.scheduling.domain.entity;

import com.pb.scheduling.domain.enums.GameSlotStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "game_slot")
public class GameSlot {

    @Id
    @Column(name = "game_slot_id", nullable = false)
    private UUID id;

    @Column(name = "arena_id", nullable = false)
    private UUID arenaId;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "capacity_companies", nullable = false)
    private int capacityCompanies;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GameSlotStatus status = GameSlotStatus.OPEN;
}
