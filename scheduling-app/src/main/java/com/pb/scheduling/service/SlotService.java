package com.pb.scheduling.service;

import com.pb.scheduling.api.dto.response.HoldSlotResponse;
import com.pb.scheduling.config.SchedulingProperties;
import com.pb.scheduling.domain.entity.GameSlot;
import com.pb.scheduling.domain.entity.SlotReservation;
import com.pb.scheduling.domain.enums.GameSlotStatus;
import com.pb.scheduling.domain.enums.SlotReservationStatus;
import com.pb.scheduling.exception.BusinessException;
import com.pb.scheduling.exception.NotFoundException;
import com.pb.scheduling.repository.GameSlotRepository;
import com.pb.scheduling.repository.SlotReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final GameSlotRepository gameSlotRepository;
    private final SlotReservationRepository slotReservationRepository;
    private final SchedulingProperties schedulingProperties;
    private final Clock clock;

    public List<GameSlot> getAllGameSlots() {
        return gameSlotRepository.findAll();
    }

    public List<SlotReservation> getAllSlotReservations() {
        return slotReservationRepository.findAll();
    }

    @Transactional
    public HoldSlotResponse holdSlot(UUID slotId, UUID bookingId, boolean isExclusive) {
        GameSlot slot = gameSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> NotFoundException.of(
                        "SLOT_NOT_FOUND",
                        "Slot not found",
                        Map.of("slotId", slotId)
                ));

        if (slot.getStatus() != GameSlotStatus.OPEN) {
            throw BusinessException.of("SLOT_NOT_OPEN", "Slot is not open", Map.of("slotId", slotId));
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        // BUG-7: запрещаем бронировать слот, который уже закончился
        if (!slot.getEndTime().isAfter(now)) {
            throw BusinessException.of("SLOT_ALREADY_FINISHED", "Slot is already in the past",
                    Map.of("slotId", slotId, "endTime", slot.getEndTime()));
        }
        long active = slotReservationRepository.countActive(slotId, now);
        if (active >= slot.getCapacityCompanies()) {
            throw BusinessException.of("SLOT_FULL", "No capacity for slot", Map.of("slotId", slotId));
        }

        if (isExclusive && active > 0) {
            throw BusinessException.of("SLOT_EXCLUSIVE",
                    "Exclusive booking requires an empty slot",
                    Map.of("slotId", slotId));
        }
        if (!isExclusive && slotReservationRepository.hasActiveExclusive(slotId, now)) {
            throw BusinessException.of("SLOT_EXCLUSIVE",
                    "Slot is occupied by an exclusive booking",
                    Map.of("slotId", slotId));
        }

        OffsetDateTime expiresAt = now.plusMinutes(Math.max(1, schedulingProperties.holdMinutes()));

        SlotReservation r = new SlotReservation(
                UUID.randomUUID(),
                slotId,
                bookingId,
                SlotReservationStatus.HOLD,
                now,
                expiresAt,
                isExclusive
        );

        slotReservationRepository.save(r);
        return new HoldSlotResponse(r.getId(), expiresAt);
    }

    @Transactional
    public void confirmHold(UUID reservationId, UUID bookingId) {
        // BUG-1: используем пессимистическую блокировку
        SlotReservation r = slotReservationRepository.findByIdAndBookingIdForUpdate(reservationId, bookingId)
                .orElseThrow(() -> NotFoundException.of(
                        "SLOT_HOLD_NOT_FOUND",
                        "Hold not found",
                        Map.of("slotReservationId", reservationId)
                ));

        if (r.getStatus() != SlotReservationStatus.HOLD) {
            throw BusinessException.of("INVALID_STATUS", "Only HOLD can be confirmed",
                    Map.of("slotReservationId", reservationId, "status", r.getStatus().name()));
        }

        if (r.getExpiresAt() == null || r.getExpiresAt().isBefore(OffsetDateTime.now(clock))) {
            throw BusinessException.of("HOLD_EXPIRED", "Hold is expired", Map.of("slotReservationId", reservationId));
        }

        r.setStatus(SlotReservationStatus.CONFIRMED);
        r.setExpiresAt(null);
        slotReservationRepository.save(r);
    }

    @Transactional
    public void cancelHold(UUID reservationId, UUID bookingId) {
        // BUG-1: используем пессимистическую блокировку
        SlotReservation r = slotReservationRepository.findByIdAndBookingIdForUpdate(reservationId, bookingId)
                .orElseThrow(() -> NotFoundException.of(
                        "SLOT_HOLD_NOT_FOUND",
                        "Hold not found",
                        Map.of("slotReservationId", reservationId)
                ));

        if (r.getStatus() == SlotReservationStatus.CANCELLED) {
            return; // идемпотентно
        }

        r.setStatus(SlotReservationStatus.CANCELLED);
        r.setExpiresAt(null); // BUG-2: очищаем expiresAt
        slotReservationRepository.save(r);
    }
}
