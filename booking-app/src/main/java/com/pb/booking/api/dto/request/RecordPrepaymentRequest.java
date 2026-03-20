package com.pb.booking.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecordPrepaymentRequest {

    @NotNull
    @Positive
    private BigDecimal prepaidAmount;
}
