package com.pb.booking.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class SubmitBookingRequest {

    // Client info — auto find-or-create
    @NotBlank
    private String clientName;

    @NotBlank
    private String clientPhone;

    @Email
    private String clientEmail;

    // Booking info
    @NotNull
    @Positive
    private Integer playersCount;

    // Type A: desired date only (admin assigns slot later)
    private LocalDate desiredDate;

    // Type B: full booking (all nullable — admin fills in at confirmation if not provided)
    private UUID gameSlotId;
    private UUID tariffId;
    private UUID zoneId;
    private BigDecimal totalPriceSnapshot;
    private Integer extraEquipmentCount;
    private boolean exclusive;
}
