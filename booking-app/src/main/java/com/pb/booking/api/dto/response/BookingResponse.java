package com.pb.booking.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {

    private UUID id;
    private UUID clientId;
    private UUID gameSlotId;
    private UUID tariffId;
    private int playersCount;
    private BigDecimal totalPriceSnapshot;
    private String status;
    private UUID slotReservationId;
    private UUID zoneReservationId;
    private String cancelReason;
    private LocalDate desiredDate;
    private Integer extraEquipmentCount;
    private BigDecimal prepaidAmount;
    private String adminNotes;
    private Instant holdExpiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
