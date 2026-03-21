package com.pb.booking.api.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateBookingRequest {

    @Positive
    private Integer playersCount;

    private LocalDate desiredDate;

    private UUID tariffId;

    @Positive
    private BigDecimal totalPriceSnapshot;

    @Positive
    private Integer extraEquipmentCount;

    private String adminNotes;
    private Boolean exclusive;

    private UUID gameSlotId;
    private UUID zoneId;
}
