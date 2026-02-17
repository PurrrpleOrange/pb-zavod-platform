package com.pb.scheduling.api.controller;

import com.pb.scheduling.api.dto.request.ConfirmByBookingRequest;
import com.pb.scheduling.api.dto.request.ExtendZoneRequest;
import com.pb.scheduling.api.dto.request.HoldZoneRequest;
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
    public Map<String, Object> hold(@PathVariable UUID zoneId, @Valid @RequestBody HoldZoneRequest req) {
        UUID id = zoneService.holdZone(zoneId, req.getBookingId(), req.getStartTime(), req.getEndTime());
        return Map.of("zoneReservationId", id);
    }

    @PostMapping("/zone-holds/{zoneReservationId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable UUID zoneReservationId, @Valid @RequestBody ConfirmByBookingRequest req) {
        zoneService.confirmZoneHold(zoneReservationId, req.getBookingId());
    }

    @PostMapping("/zone-holds/{zoneReservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID zoneReservationId, @Valid @RequestBody ConfirmByBookingRequest req) {
        zoneService.cancelZoneHold(zoneReservationId, req.getBookingId());
    }

    @PostMapping("/zone-reservations/{zoneReservationId}/extend")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> extend(@PathVariable UUID zoneReservationId,
                                      @Valid @RequestBody ExtendZoneRequest req,
                                      @RequestParam UUID bookingId) {
        UUID id = zoneService.extend(zoneReservationId, bookingId, req.getNewEndTime(), req.getExtendMinutes());
        return Map.of("zoneReservationId", id);
    }
}
