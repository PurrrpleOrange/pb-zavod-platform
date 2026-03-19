package com.pb.booking.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateBookingRequest {

    @NotNull
    private UUID clientId;

    @NotNull
    private UUID gameSlotId;

    @NotNull
    private UUID tariffId;

    @NotNull
    @Positive
    private Integer playersCount;

    @NotNull
    @Positive
    private BigDecimal totalPriceSnapshot;

    @NotNull
    private UUID zoneId;
}
