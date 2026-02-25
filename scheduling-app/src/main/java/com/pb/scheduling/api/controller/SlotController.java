package com.pb.scheduling.api.controller;

import com.pb.scheduling.api.dto.request.ConfirmByBookingRequest;
import com.pb.scheduling.api.dto.request.HoldSlotRequest;
import com.pb.scheduling.api.dto.response.HoldSlotResponse;
import com.pb.scheduling.domain.entity.GameSlot;
import com.pb.scheduling.domain.entity.SlotReservation;
import com.pb.scheduling.service.SlotService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @GetMapping("/slots/getAll")
    public List<GameSlot> getAllGameSlots() {
        return slotService.getAllGameSlots();
    }

    @GetMapping("/slot-resrv/getAll")
    public List<SlotReservation> getAllSlotReservations() {
        return slotService.getAllSlotReservations();
    }

    @PostMapping("/slots/{slotId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldSlotResponse hold(
            @PathVariable("slotId") UUID slotId,
            @Valid @RequestBody HoldSlotRequest req) {
        return slotService.holdSlot(slotId, req.getBookingId());
    }

    @PostMapping("/slot-holds/{slotReservationId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(
            @PathVariable("slotReservationId") UUID slotReservationId,
            @Valid @RequestBody ConfirmByBookingRequest req) {
        slotService.confirmHold(slotReservationId, req.getBookingId());
    }

    @PostMapping("/slot-holds/{slotReservationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable("slotReservationId") UUID slotReservationId,
            @Valid @RequestBody ConfirmByBookingRequest req) {
        slotService.cancelHold(slotReservationId, req.getBookingId());
    }
}
