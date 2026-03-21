package com.pb.booking.api.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ConfirmBookingRequest {

    private UUID gameSlotId;
    private UUID tariffId;
    private UUID zoneId;
    private BigDecimal totalPriceSnapshot;
    private Integer extraEquipmentCount;
    private String adminNotes;
    private Boolean exclusive;
}
