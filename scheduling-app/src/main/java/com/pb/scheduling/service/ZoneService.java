package com.pb.scheduling.service;

import com.pb.scheduling.api.dto.request.CreateZoneRequest;
import com.pb.scheduling.api.dto.request.UpdateZoneRequest;
import com.pb.scheduling.api.dto.response.HoldZoneResponse;
import com.pb.scheduling.api.dto.response.ZoneReservationResponse;
import com.pb.scheduling.api.dto.response.ZoneResponse;
import com.pb.scheduling.config.SchedulingProperties;
import com.pb.scheduling.domain.entity.GameSlot;
import com.pb.scheduling.domain.entity.SlotReservation;
import com.pb.scheduling.domain.entity.Zone;
import com.pb.scheduling.domain.entity.ZoneReservation;
import com.pb.scheduling.domain.enums.SlotReservationStatus;
import com.pb.scheduling.domain.enums.ZoneReservationStatus;
import com.pb.scheduling.exception.BusinessException;
import com.pb.scheduling.exception.NotFoundException;
import com.pb.scheduling.repository.GameSlotRepository;
import com.pb.scheduling.repository.SlotReservationRepository;
import com.pb.scheduling.repository.ZoneRepository;
import com.pb.scheduling.repository.ZoneReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final SlotReservationRepository slotReservationRepository;
    private final ZoneReservationRepository zoneReservationRepository;
    private final GameSlotRepository gameSlotRepository;
    private final SchedulingProperties schedulingProperties;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public ZoneResponse createZone(CreateZoneRequest req) {
        if (zoneRepository.existsByCode(req.getCode())) {
            throw BusinessException.of("ZONE_CODE_DUPLICATE",
                    "Zone with code '" + req.getCode() + "' already exists");
        }
        Zone zone = new Zone(
                UUID.randomUUID(),
                req.getCode(),
                req.getName(),
                req.getType(),
                req.getCapacityCompanies(),
                req.isPaid(),
                true
        );
        return toResponse(zoneRepository.save(zone));
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getAllZones(Boolean active) {
        List<Zone> zones = active != null
                ? zoneRepository.findByActive(active)
                : zoneRepository.findAll();
        return zones.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ZoneResponse getZone(UUID zoneId) {
        return toResponse(findOrThrow(zoneId));
    }

    @Transactional
    public ZoneResponse updateZone(UUID zoneId, UpdateZoneRequest req) {
        Zone zone = findOrThrow(zoneId);
        if (req.getName() != null) zone.setName(req.getName());
        if (req.getType() != null) zone.setType(req.getType());
        if (req.getCapacityCompanies() != null) zone.setCapacityCompanies(req.getCapacityCompanies());
        if (req.getPaid() != null) zone.setPaid(req.getPaid());
        if (req.getActive() != null) zone.setActive(req.getActive());
        return toResponse(zoneRepository.save(zone));
    }

    @Transactional
    public void deactivateZone(UUID zoneId) {
        Zone zone = findOrThrow(zoneId);
        zone.setActive(false);
        zoneRepository.save(zone);
    }

    private Zone findOrThrow(UUID zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> NotFoundException.of(
                        "ZONE_NOT_FOUND", "Zone not found", Map.of("zoneId", zoneId)));
    }

    private ZoneResponse toResponse(Zone zone) {
        return ZoneResponse.builder()
                .id(zone.getId())
                .code(zone.getCode())
                .name(zone.getName())
                .type(zone.getType())
                .capacityCompanies(zone.getCapacityCompanies())
                .paid(zone.isPaid())
                .active(zone.isActive())
                .build();
    }

    // ── ZONE RESERVATIONS READ ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ZoneReservationResponse getZoneReservation(UUID zoneReservationId) {
        ZoneReservation r = zoneReservationRepository.findById(zoneReservationId)
                .orElseThrow(() -> NotFoundException.of("ZONE_RES_NOT_FOUND", "Zone reservation not found",
                        Map.of("zoneReservationId", zoneReservationId)));
        return toReservationResponse(r);
    }

    @Transactional(readOnly = true)
    public List<ZoneReservationResponse> listZoneReservations(UUID zoneId, UUID bookingId, ZoneReservationStatus status) {
        return zoneReservationRepository.findAllFiltered(zoneId, bookingId, status)
                .stream().map(this::toReservationResponse).toList();
    }

    private ZoneReservationResponse toReservationResponse(ZoneReservation r) {
        return ZoneReservationResponse.builder()
                .id(r.getId())
                .zoneId(r.getZoneId())
                .bookingId(r.getBookingId())
                .slotReservationId(r.getSlotReservationId())
                .parentReservationId(r.getParentReservationId())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .status(r.getStatus())
                .expiresAt(r.getExpiresAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    // ── HOLDS ─────────────────────────────────────────────────────────────────

    @Transactional
    public HoldZoneResponse holdZone(UUID zoneId,
                                     UUID bookingId,
                                     UUID slotReservationId) {
        // проверяем, что zone существует
        Zone zone = zoneRepository.findByIdForUpdate(zoneId)
                .orElseThrow(() -> NotFoundException.of(
                        "ZONE_NOT_FOUND", "Zone not found", Map.of("zoneId", zoneId)
                ));

        SlotReservation slotReservation = slotReservationRepository.findByIdLocked(slotReservationId)
                .orElseThrow(() -> NotFoundException.of(
                        "SLOT_RES_NOT_FOUND", "Slot reservation not found", Map.of("slotReservationId", slotReservationId)
                ));

        // проверяем, что bookingId совпадает
        if(!slotReservation.getBookingId().equals(bookingId)) {
            throw BusinessException.of("BOOKING_MISMATCH", "bookingId mismatch",
                    Map.of("slotReservationId", slotReservationId));
        }

        // проверяем, что статус slot reservation - HOLD или CONFIRMED
        if(slotReservation.getStatus()!= SlotReservationStatus.HOLD &&
                slotReservation.getStatus()!= SlotReservationStatus.CONFIRMED) {
            throw BusinessException.of("SLOT_RES_STATUS_MISMATCH", "slot reservation status mismatch",
                    Map.of("slotReservationId", slotReservationId));
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (slotReservation.getStatus() == SlotReservationStatus.HOLD &&
                (slotReservation.getExpiresAt() == null || !slotReservation.getExpiresAt().isAfter(now))) {
            throw BusinessException.of("SLOT_HOLD_EXPIRED", "Slot hold expired",
                    Map.of("slotReservationId", slotReservationId, "expiresAt", slotReservation.getExpiresAt()));
        }

        // проверяем, что zone активна
        if (!zone.isActive()) {
            throw BusinessException.of("ZONE_INACTIVE", "Zone is inactive", Map.of("zoneId", zoneId));
        }

        // проверяем, что game slot ещё не закончился
        Optional<GameSlot> gameSlot = gameSlotRepository.findByIdForUpdate(slotReservation.getGameSlotId());
        if (gameSlot.isEmpty()) {
            throw NotFoundException.of(
                    "GAME_SLOT_NOT_FOUND", "Game slot not found", Map.of("gameSlotId", slotReservation.getGameSlotId())
            );
        }
        if(!gameSlot.get().getEndTime().isAfter(now)) {
            throw BusinessException.of(
                    "SLOT_ALREADY_FINISHED",
                    "game slot already finished",
                    Map.of(
                            "slotId", gameSlot.get().getId(),
                            "endTime", gameSlot.get().getEndTime()
                    )
            );
        }

        OffsetDateTime start = gameSlot.get().getStartTime();
        OffsetDateTime end = start.plusMinutes(Math.max(1, schedulingProperties.defaultDurationMinutesForZones()));
        OffsetDateTime expiresAt = now.plusMinutes(Math.max(1, schedulingProperties.holdMinutes()));

        long overlaps = zoneReservationRepository.countOverlaps(
                zoneId, start, end, now, null
        );

        if (overlaps >= zone.getCapacityCompanies()) {
            throw BusinessException.of("ZONE_BUSY", "Zone is busy",
                    Map.of("zoneId", zoneId, "startTime", start, "endTime", end));
        }

        ZoneReservation r = new ZoneReservation(
                UUID.randomUUID(),
                zoneId,
                bookingId,
                start,
                end,
                ZoneReservationStatus.HOLD,
                null,      // parentReservationId
                expiresAt,  // expiresAt
                slotReservationId
        );

        zoneReservationRepository.save(r);
        return new HoldZoneResponse(r.getId(), expiresAt);
    }

    @Transactional
    public void confirmZoneHold(UUID zoneReservationId, UUID bookingId) {
        ZoneReservation r = zoneReservationRepository.findByIdForUpdate(zoneReservationId)
                .orElseThrow(() -> NotFoundException.of("ZONE_HOLD_NOT_FOUND", "Zone hold not found",
                        Map.of("zoneReservationId", zoneReservationId)));

        if (!r.getBookingId().equals(bookingId)) {
            throw BusinessException.of("BOOKING_MISMATCH", "bookingId mismatch",
                    Map.of("zoneReservationId", zoneReservationId));
        }

        if (r.getStatus() != ZoneReservationStatus.HOLD) {
            throw BusinessException.of("INVALID_STATUS", "Only HOLD can be confirmed",
                    Map.of("zoneReservationId", zoneReservationId, "status", r.getStatus().name()));
        }

        // истёкший HOLD нельзя подтверждать
        OffsetDateTime now = OffsetDateTime.now();
        if (r.getExpiresAt() == null || !r.getExpiresAt().isAfter(now)) {
            throw BusinessException.of("HOLD_EXPIRED", "Zone hold expired",
                    Map.of("zoneReservationId", zoneReservationId, "expiresAt", r.getExpiresAt()));
        }

        Zone zone = zoneRepository.findByIdForUpdate(r.getZoneId())
                .orElseThrow(() -> NotFoundException.of("ZONE_NOT_FOUND", "Zone not found",
                        Map.of("zoneId", r.getZoneId())));

        long overlaps = zoneReservationRepository.countOverlaps(
                r.getZoneId(), r.getStartTime(), r.getEndTime(), now, r.getId()
        );

        if (overlaps >= zone.getCapacityCompanies()) {
            throw BusinessException.of("ZONE_BUSY", "Zone is busy",
                    Map.of("zoneId", r.getZoneId(), "zoneReservationId", zoneReservationId));
        }

        r.setStatus(ZoneReservationStatus.ACTIVE);
        // можно обнулить expiresAt, чтобы не путаться
        r.setExpiresAt(null);
        zoneReservationRepository.save(r);
    }

    @Transactional
    public void cancelZoneHold(UUID zoneReservationId, UUID bookingId) {
        ZoneReservation r = zoneReservationRepository.findByIdForUpdate(zoneReservationId)
                .orElseThrow(() -> NotFoundException.of("ZONE_RES_NOT_FOUND", "Zone reservation not found",
                        Map.of("zoneReservationId", zoneReservationId)));

        if (!r.getBookingId().equals(bookingId)) {
            throw BusinessException.of("BOOKING_MISMATCH", "bookingId mismatch",
                    Map.of("zoneReservationId", zoneReservationId));
        }

        if (r.getStatus() == ZoneReservationStatus.CANCELLED) {
            return; // идемпотентно
        }

        r.setStatus(ZoneReservationStatus.CANCELLED);
        r.setExpiresAt(null);
        zoneReservationRepository.save(r);
    }

    @Transactional
    public UUID extend(UUID zoneReservationId, UUID bookingId, Integer extendMinutes) {
        ZoneReservation base = zoneReservationRepository.findByIdForUpdate(zoneReservationId)
                .orElseThrow(() -> NotFoundException.of("ZONE_RES_NOT_FOUND", "Zone reservation not found",
                        Map.of("zoneReservationId", zoneReservationId)));

        if (!base.getBookingId().equals(bookingId)) {
            throw BusinessException.of("BOOKING_MISMATCH", "bookingId mismatch",
                    Map.of("zoneReservationId", zoneReservationId));
        }

        if (base.getStatus() != ZoneReservationStatus.ACTIVE) {
            throw BusinessException.of("INVALID_STATUS", "Only ACTIVE reservation can be extended",
                    Map.of("zoneReservationId", zoneReservationId, "status", base.getStatus().name()));
        }

        OffsetDateTime targetEnd;
        if (extendMinutes != null) {
            targetEnd = base.getEndTime().plusMinutes(extendMinutes);
        } else {
            throw BusinessException.of("INVALID_REQUEST", "Provide extendMinutes", Map.of());
        }

        if (!targetEnd.isAfter(base.getEndTime())) {
            throw BusinessException.of("INVALID_TIME", "Extend cannot reduce endTime",
                    Map.of("currentEndTime", base.getEndTime(), "targetEndTime", targetEnd));
        }

        Zone zone = zoneRepository.findByIdForUpdate(base.getZoneId())
                .orElseThrow(() -> NotFoundException.of("ZONE_NOT_FOUND", "Zone not found",
                        Map.of("zoneId", base.getZoneId())));

        OffsetDateTime now = OffsetDateTime.now();
        long overlaps = zoneReservationRepository.countOverlaps(
                base.getZoneId(),
                base.getEndTime(),
                targetEnd,
                now,
                null
        );

        if (overlaps >= zone.getCapacityCompanies()) {
            throw BusinessException.of("ZONE_BUSY", "Zone is busy for extension",
                    Map.of("zoneId", base.getZoneId(), "from", base.getEndTime(), "to", targetEnd));
        }

        ZoneReservation ext = new ZoneReservation(
                UUID.randomUUID(),
                base.getZoneId(),
                bookingId,
                base.getEndTime(),
                targetEnd,
                ZoneReservationStatus.ACTIVE,
                now,
                base.getId(), // parentReservationId
                null,          // expiresAt
                base.getSlotReservationId()
        );

        zoneReservationRepository.save(ext);
        return ext.getId();
    }
}
