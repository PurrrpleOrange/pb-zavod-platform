package com.pb.scheduling.service;

import com.pb.scheduling.api.dto.response.HoldZoneResponse;
import com.pb.scheduling.config.SchedulingProperties;
import com.pb.scheduling.domain.entity.Zone;
import com.pb.scheduling.domain.entity.ZoneReservation;
import com.pb.scheduling.domain.enums.ZoneReservationStatus;
import com.pb.scheduling.exception.BusinessException;
import com.pb.scheduling.exception.NotFoundException;
import com.pb.scheduling.repository.ZoneRepository;
import com.pb.scheduling.repository.ZoneReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final ZoneReservationRepository zoneReservationRepository;
    private final SchedulingProperties schedulingProperties;

    @Transactional
    public HoldZoneResponse holdZone(UUID zoneId,
                                     UUID bookingId,
                                     OffsetDateTime start,
                                     OffsetDateTime end) {

        if (start == null || end == null) {
            throw BusinessException.of("INVALID_TIME", "startTime/endTime must be provided",
                    Map.of("startTime", start, "endTime", end));
        }
        if (!end.isAfter(start)) {
            throw BusinessException.of("INVALID_TIME", "endTime must be after startTime",
                    Map.of("startTime", start, "endTime", end));
        }

        Zone zone = zoneRepository.findByIdForUpdate(zoneId)
                .orElseThrow(() -> NotFoundException.of(
                        "ZONE_NOT_FOUND", "Zone not found", Map.of("zoneId", zoneId)
                ));

        if (!zone.isActive()) {
            throw BusinessException.of("ZONE_INACTIVE", "Zone is inactive", Map.of("zoneId", zoneId));
        }

        OffsetDateTime now = OffsetDateTime.now();
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
                now,
                null,      // parentReservationId
                expiresAt  // expiresAt
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
    public UUID extend(UUID zoneReservationId, UUID bookingId, OffsetDateTime newEndTime, Integer extendMinutes) {
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
        if (newEndTime != null) {
            targetEnd = newEndTime;
        } else if (extendMinutes != null) {
            targetEnd = base.getEndTime().plusMinutes(extendMinutes);
        } else {
            throw BusinessException.of("INVALID_REQUEST", "Provide newEndTime or extendMinutes", Map.of());
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
                null          // expiresAt
        );

        zoneReservationRepository.save(ext);
        return ext.getId();
    }
}
