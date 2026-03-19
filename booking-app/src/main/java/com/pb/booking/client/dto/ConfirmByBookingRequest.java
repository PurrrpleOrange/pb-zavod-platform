package com.pb.booking.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ConfirmByBookingRequest {

    private UUID bookingId;
}
