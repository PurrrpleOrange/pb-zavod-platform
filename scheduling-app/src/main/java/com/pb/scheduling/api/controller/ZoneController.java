package com.pb.scheduling.api.controller;

import com.pb.scheduling.api.dto.request.ConfirmByBookingRequest;
import com.pb.scheduling.api.dto.request.ExtendZoneRequest;
import com.pb.scheduling.api.dto.request.HoldZoneRequest;
import com.pb.scheduling.api.dto.response.HoldZoneResponse;
import com.pb.scheduling.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

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
            @PathVariable("zoneReservationId") UUID zoneReservationId,
            @Valid @RequestBody ConfirmByBookingRequest req) {
        zoneService.confirmZoneHold(zoneReservationId, req.getBookingId());
    }

    @PostMapping("/zone-holds/{zoneReservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable("zoneReservationId") UUID zoneReservationId,
            @Valid @RequestBody ConfirmByBookingRequest req) {
        zoneService.cancelZoneHold(zoneReservationId, req.getBookingId());
    }

    @PostMapping("/zone-reservations/{zoneReservationId}/extend")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID extend(@PathVariable("zoneReservationId") UUID zoneReservationId,
                       @Valid @RequestBody ExtendZoneRequest req) {
        return zoneService.extend(
                zoneReservationId,
                req.getBookingId(),
                req.getExtendMinutes()
        );
    }
}
