package com.pb.booking.domain.entity;

import com.pb.booking.domain.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "booking", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id")
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "game_slot_id")
    private UUID gameSlotId;

    @Column(name = "tariff_id")
    private UUID tariffId;

    @Column(name = "players_count", nullable = false)
    private int playersCount;

    @Column(name = "total_price_snapshot", precision = 12, scale = 2)
    private BigDecimal totalPriceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "slot_reservation_id")
    private UUID slotReservationId;

    @Column(name = "zone_reservation_id")
    private UUID zoneReservationId;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "desired_date")
    private LocalDate desiredDate;

    @Column(name = "extra_equipment_count")
    private Integer extraEquipmentCount;

    @Column(name = "prepaid_amount", precision = 12, scale = 2)
    private BigDecimal prepaidAmount;

    @Column(name = "admin_notes")
    private String adminNotes;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    private int version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
