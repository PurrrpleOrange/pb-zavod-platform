package com.pb.scheduling.domain.entity;

import com.pb.scheduling.domain.enums.SlotReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "slot_reservation")
public class SlotReservation {

    @Id
    @Column(name = "slot_reservation_id", nullable = false)
    private UUID id;

    @Column(name = "game_slot_id", nullable = false)
    private UUID gameSlotId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlotReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "is_exclusive", nullable = false)
    private boolean exclusive;
}
