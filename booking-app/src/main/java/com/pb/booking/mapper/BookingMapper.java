package com.pb.booking.mapper;

import com.pb.booking.api.dto.response.BookingResponse;
import com.pb.booking.domain.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .clientId(booking.getClientId())
                .gameSlotId(booking.getGameSlotId())
                .tariffId(booking.getTariffId())
                .playersCount(booking.getPlayersCount())
                .totalPriceSnapshot(booking.getTotalPriceSnapshot())
                .status(booking.getStatus().name())
                .slotReservationId(booking.getSlotReservationId())
                .zoneReservationId(booking.getZoneReservationId())
                .cancelReason(booking.getCancelReason())
                .desiredDate(booking.getDesiredDate())
                .extraEquipmentCount(booking.getExtraEquipmentCount())
                .prepaidAmount(booking.getPrepaidAmount())
                .adminNotes(booking.getAdminNotes())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
