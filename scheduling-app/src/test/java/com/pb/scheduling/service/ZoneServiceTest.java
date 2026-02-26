package com.pb.scheduling.service;

import com.pb.scheduling.api.dto.response.HoldZoneResponse;
import com.pb.scheduling.config.SchedulingProperties;
import com.pb.scheduling.domain.entity.GameSlot;
import com.pb.scheduling.domain.entity.SlotReservation;
import com.pb.scheduling.domain.entity.Zone;
import com.pb.scheduling.domain.entity.ZoneReservation;
import com.pb.scheduling.domain.enums.GameSlotStatus;
import com.pb.scheduling.domain.enums.SlotReservationStatus;
import com.pb.scheduling.domain.enums.ZoneReservationStatus;
import com.pb.scheduling.domain.enums.ZoneType;
import com.pb.scheduling.exception.BusinessException;
import com.pb.scheduling.repository.GameSlotRepository;
import com.pb.scheduling.repository.SlotReservationRepository;
import com.pb.scheduling.repository.ZoneRepository;
import com.pb.scheduling.repository.ZoneReservationRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private SlotReservationRepository slotReservationRepository;
    @Mock
    private ZoneReservationRepository zoneReservationRepository;
    @Mock
    private GameSlotRepository gameSlotRepository;

    private ZoneService zoneService;

    @BeforeEach
    void setUp() {
        zoneService = new ZoneService(
                zoneRepository,
                slotReservationRepository,
                zoneReservationRepository,
                gameSlotRepository,
                new SchedulingProperties(15, 180)
        );
    }

    @Test
    void holdZone_throwsZoneBusy_whenOverlapsReachCapacity() {
        UUID zoneId = UUID.randomUUID();
        UUID slotReservationId = UUID.randomUUID();

        Zone zone = new Zone(zoneId, "Z-1", "Rest", ZoneType.REST, 1, true, true);
        SlotReservation slotReservation = new SlotReservation(
                slotReservationId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                SlotReservationStatus.CONFIRMED,
                OffsetDateTime.now(),
                null
        );

        GameSlot gameSlot = new GameSlot(
                slotReservation.getGameSlotId(),
                UUID.randomUUID(),
                OffsetDateTime.now().plusMinutes(30),
                OffsetDateTime.now().plusHours(2),
                2,
                GameSlotStatus.OPEN
        );

        when(zoneRepository.findByIdForUpdate(zoneId)).thenReturn(Optional.of(zone));
        when(slotReservationRepository.findByIdLocked(slotReservationId)).thenReturn(Optional.of(slotReservation));
        when(gameSlotRepository.findByIdForUpdate(slotReservation.getGameSlotId())).thenReturn(Optional.of(gameSlot));
        when(zoneReservationRepository.countOverlaps(eq(zoneId), any(), any(), any(), eq(null))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> zoneService.holdZone(zoneId, UUID.randomUUID(), slotReservationId));

        assertEquals("ZONE_BUSY", ex.getCode());
        verify(zoneReservationRepository, never()).save(any());
    }

    @Test
    void holdZone_success_createsHoldReservation() {
        UUID zoneId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID slotReservationId = UUID.randomUUID();

        Zone zone = new Zone(zoneId, "Z-2", "Dressing", ZoneType.DRESSING, 2, false, true);
        SlotReservation slotReservation = new SlotReservation(
                slotReservationId,
                UUID.randomUUID(),
                bookingId,
                SlotReservationStatus.CONFIRMED,
                OffsetDateTime.now(),
                null
        );

        GameSlot gameSlot = new GameSlot(
                slotReservation.getGameSlotId(),
                UUID.randomUUID(),
                OffsetDateTime.now().plusMinutes(10),
                OffsetDateTime.now().plusHours(3),
                2,
                GameSlotStatus.OPEN
        );

        when(zoneRepository.findByIdForUpdate(zoneId)).thenReturn(Optional.of(zone));
        when(slotReservationRepository.findByIdLocked(slotReservationId)).thenReturn(Optional.of(slotReservation));
        when(gameSlotRepository.findByIdForUpdate(slotReservation.getGameSlotId())).thenReturn(Optional.of(gameSlot));
        when(zoneReservationRepository.countOverlaps(eq(zoneId), any(), any(), any(), eq(null))).thenReturn(0L);

        HoldZoneResponse response = zoneService.holdZone(zoneId, bookingId, slotReservationId);

        assertNotNull(response.getZoneReservationId());
        assertTrue(response.getExpiresAt().isAfter(OffsetDateTime.now()));

        ArgumentCaptor<ZoneReservation> captor = ArgumentCaptor.forClass(ZoneReservation.class);
        verify(zoneReservationRepository).save(captor.capture());
        ZoneReservation saved = captor.getValue();
        assertEquals(zoneId, saved.getZoneId());
        assertEquals(bookingId, saved.getBookingId());
        assertEquals(ZoneReservationStatus.HOLD, saved.getStatus());
        assertEquals(slotReservationId, saved.getSlotReservationId());
    }

    @Test
    void extend_throwsInvalidStatus_whenBaseNotActive() {
        UUID reservationId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        ZoneReservation base = new ZoneReservation(
                reservationId,
                UUID.randomUUID(),
                bookingId,
                OffsetDateTime.now().plusMinutes(10),
                OffsetDateTime.now().plusHours(1),
                ZoneReservationStatus.HOLD,
                OffsetDateTime.now(),
                null,
                OffsetDateTime.now().plusMinutes(5),
                UUID.randomUUID()
        );

        when(zoneReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(base));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> zoneService.extend(reservationId, bookingId, 30));

        assertEquals("INVALID_STATUS", ex.getCode());
        verify(zoneReservationRepository, never()).save(any());
    }

    @Test
    void extend_success_createsChildActiveReservation() {
        UUID reservationId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();

        ZoneReservation base = new ZoneReservation(
                reservationId,
                zoneId,
                bookingId,
                OffsetDateTime.now().minusHours(1),
                OffsetDateTime.now().plusMinutes(30),
                ZoneReservationStatus.ACTIVE,
                OffsetDateTime.now().minusHours(1),
                null,
                null,
                UUID.randomUUID()
        );

        Zone zone = new Zone(zoneId, "Z-3", "Rest", ZoneType.REST, 1, true, true);

        when(zoneReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.of(base));
        when(zoneRepository.findByIdForUpdate(zoneId)).thenReturn(Optional.of(zone));
        when(zoneReservationRepository.countOverlaps(eq(zoneId), any(), any(), any(), eq(null))).thenReturn(0L);

        UUID extId = zoneService.extend(reservationId, bookingId, 20);

        assertNotNull(extId);

        ArgumentCaptor<ZoneReservation> captor = ArgumentCaptor.forClass(ZoneReservation.class);
        verify(zoneReservationRepository).save(captor.capture());
        ZoneReservation saved = captor.getValue();
        assertEquals(ZoneReservationStatus.ACTIVE, saved.getStatus());
        assertEquals(reservationId, saved.getParentReservationId());
        assertEquals(base.getEndTime(), saved.getStartTime());
        assertTrue(saved.getEndTime().isAfter(saved.getStartTime()));
    }
}
