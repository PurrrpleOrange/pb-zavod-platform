package com.pb.scheduling.domain.entity;

import com.pb.scheduling.domain.enums.ZoneReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "zone_reservation")
public class ZoneReservation {

    @Id
    @Column(name = "zone_reservation_id", nullable = false)
    private UUID id;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ZoneReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "parent_reservation_id")
    private UUID parentReservationId;
}
