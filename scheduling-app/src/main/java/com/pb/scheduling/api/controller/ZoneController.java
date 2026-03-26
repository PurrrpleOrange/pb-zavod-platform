package com.pb.scheduling.api.controller;

import com.pb.scheduling.api.dto.request.ConfirmByBookingRequest;
import com.pb.scheduling.api.dto.request.CreateZoneRequest;
import com.pb.scheduling.api.dto.request.ExtendZoneRequest;
import com.pb.scheduling.api.dto.request.HoldZoneRequest;
import com.pb.scheduling.api.dto.request.UpdateZoneRequest;
import com.pb.scheduling.api.dto.response.HoldZoneResponse;
import com.pb.scheduling.api.dto.response.ZoneReservationResponse;
import com.pb.scheduling.api.dto.response.ZoneResponse;
import com.pb.scheduling.domain.enums.ZoneReservationStatus;
import com.pb.scheduling.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping("/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse createZone(@Valid @RequestBody CreateZoneRequest req) {
        return zoneService.createZone(req);
    }

    @GetMapping("/zones")
    public List<ZoneResponse> getAllZones(
            @RequestParam(name = "active", required = false) Boolean active) {
        return zoneService.getAllZones(active);
    }

    @GetMapping("/zones/{zoneId}")
    public ZoneResponse getZone(@PathVariable UUID zoneId) {
        return zoneService.getZone(zoneId);
    }

    @PatchMapping("/zones/{zoneId}")
    public ZoneResponse updateZone(
            @PathVariable UUID zoneId,
            @Valid @RequestBody UpdateZoneRequest req) {
        return zoneService.updateZone(zoneId, req);
    }

    @DeleteMapping("/zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateZone(@PathVariable UUID zoneId) {
        zoneService.deactivateZone(zoneId);
    }

    // ── ZONE RESERVATIONS READ ────────────────────────────────────────────────

    @GetMapping("/zone-reservations")
    public List<ZoneReservationResponse> listZoneReservations(
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) UUID bookingId,
            @RequestParam(required = false) ZoneReservationStatus status) {
        return zoneService.listZoneReservations(zoneId, bookingId, status);
    }

    @GetMapping("/zone-reservations/{zoneReservationId}")
    public ZoneReservationResponse getZoneReservation(
            @PathVariable UUID zoneReservationId) {
        return zoneService.getZoneReservation(zoneReservationId);
    }

    // ── HOLDS ─────────────────────────────────────────────────────────────────

    @PostMapping("/zones/{zoneId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldZoneResponse hold(
            @PathVariable("zoneId") UUID zoneId,
            @Valid @RequestBody HoldZoneRequest req) {
        return zoneService.holdZone(
                zoneId,
                req.getBookingId(),
                req.getSlotReservationId());
    }

    @PostMapping("/zone-holds/{zoneReservationId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(
            @PathVariable UUID zoneReservationId,
            @Valid @RequestBody ConfirmByBookingRequest req) {
        zoneService.confirmZoneHold(zoneReservationId, req.getBookingId());
    }

    @PostMapping("/zone-holds/{zoneReservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable UUID zoneReservationId,
            @Valid @RequestBody ConfirmByBookingRequest req) {
        zoneService.cancelZoneHold(zoneReservationId, req.getBookingId());
    }

    @PostMapping("/zone-reservations/{zoneReservationId}/extend")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID extend(@PathVariable UUID zoneReservationId,
                       @Valid @RequestBody ExtendZoneRequest req) {
        return zoneService.extend(
                zoneReservationId,
                req.getBookingId(),
                req.getExtendMinutes()
        );
    }

    @PostMapping("/zone-reservations/{zoneReservationId}/finish-chain")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finishChain(
            @PathVariable UUID zoneReservationId,
            @Valid @RequestBody ConfirmByBookingRequest req) {
        zoneService.finishChain(zoneReservationId, req.getBookingId());
    }
}
