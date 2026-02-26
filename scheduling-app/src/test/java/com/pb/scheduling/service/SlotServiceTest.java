package com.pb.scheduling.service;

import com.pb.scheduling.api.dto.response.HoldSlotResponse;
import com.pb.scheduling.config.SchedulingProperties;
import com.pb.scheduling.domain.entity.GameSlot;
import com.pb.scheduling.domain.entity.SlotReservation;
import com.pb.scheduling.domain.enums.GameSlotStatus;
import com.pb.scheduling.domain.enums.SlotReservationStatus;
import com.pb.scheduling.exception.BusinessException;
import com.pb.scheduling.repository.GameSlotRepository;
import com.pb.scheduling.repository.SlotReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock
    private GameSlotRepository gameSlotRepository;
    @Mock
    private SlotReservationRepository slotReservationRepository;

    private SlotService slotService;

    @BeforeEach
    void setUp() {
        slotService = new SlotService(gameSlotRepository, slotReservationRepository, new SchedulingProperties(15, 180));
    }

    @Test
    void holdSlot_success_whenOpenAndCapacityAvailable() {
        UUID slotId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        GameSlot slot = new GameSlot(
                slotId,
                UUID.randomUUID(),
                OffsetDateTime.now().plusHours(1),
                OffsetDateTime.now().plusHours(2),
                2,
                GameSlotStatus.OPEN
        );

        when(gameSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(slotReservationRepository.countActive(eq(slotId), any())).thenReturn(1L);

        HoldSlotResponse response = slotService.holdSlot(slotId, bookingId);

        assertNotNull(response);
        assertNotNull(response.getSlotReservationId());
        assertTrue(response.getExpiresAt().isAfter(OffsetDateTime.now()));

        ArgumentCaptor<SlotReservation> captor = ArgumentCaptor.forClass(SlotReservation.class);
        verify(slotReservationRepository).save(captor.capture());
        SlotReservation saved = captor.getValue();
        assertEquals(slotId, saved.getGameSlotId());
        assertEquals(bookingId, saved.getBookingId());
        assertEquals(SlotReservationStatus.HOLD, saved.getStatus());
        assertNotNull(saved.getExpiresAt());
    }

    @Test
    void holdSlot_throwsSlotFull_whenCapacityExceeded() {
        UUID slotId = UUID.randomUUID();

        GameSlot slot = new GameSlot(
                slotId,
                UUID.randomUUID(),
                OffsetDateTime.now().plusHours(1),
                OffsetDateTime.now().plusHours(2),
                1,
                GameSlotStatus.OPEN
        );

        when(gameSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(slotReservationRepository.countActive(eq(slotId), any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> slotService.holdSlot(slotId, UUID.randomUUID()));
        assertEquals("SLOT_FULL", ex.getCode());
        verify(slotReservationRepository, never()).save(any());
    }

    @Test
    void confirmHold_throwsHoldExpired_whenExpiresAtInPast() {
        UUID reservationId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        SlotReservation hold = new SlotReservation(
                reservationId,
                UUID.randomUUID(),
                bookingId,
                SlotReservationStatus.HOLD,
                OffsetDateTime.now().minusMinutes(30),
                OffsetDateTime.now().minusMinutes(1)
        );

        when(slotReservationRepository.findByIdAndBookingId(reservationId, bookingId)).thenReturn(Optional.of(hold));

        BusinessException ex = assertThrows(BusinessException.class, () -> slotService.confirmHold(reservationId, bookingId));
        assertEquals("HOLD_EXPIRED", ex.getCode());
        verify(slotReservationRepository, never()).save(any());
    }

    @Test
    void cancelHold_isIdempotent_whenAlreadyCancelled() {
        UUID reservationId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        SlotReservation cancelled = new SlotReservation(
                reservationId,
                UUID.randomUUID(),
                bookingId,
                SlotReservationStatus.CANCELLED,
                OffsetDateTime.now(),
                null
        );

        when(slotReservationRepository.findByIdAndBookingId(reservationId, bookingId)).thenReturn(Optional.of(cancelled));

        slotService.cancelHold(reservationId, bookingId);

        verify(slotReservationRepository, never()).save(any());
    }
}
