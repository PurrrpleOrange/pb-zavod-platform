package com.pb.booking.api.controller;

import com.pb.booking.api.dto.request.CancelBookingRequest;
import com.pb.booking.api.dto.request.ConfirmBookingRequest;
import com.pb.booking.api.dto.request.RecordPrepaymentRequest;
import com.pb.booking.api.dto.request.SubmitBookingRequest;
import com.pb.booking.api.dto.response.BookingResponse;
import com.pb.booking.domain.entity.Booking;
import com.pb.booking.domain.enums.BookingStatus;
import com.pb.booking.mapper.BookingMapper;
import com.pb.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @PostMapping
    public ResponseEntity<BookingResponse> submitBooking(@Valid @RequestBody SubmitBookingRequest request) {
        Booking booking = bookingService.submitBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingMapper.toResponse(booking));
    }

    @PostMapping("/{id}/prepayment")
    public ResponseEntity<BookingResponse> recordPrepayment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RecordPrepaymentRequest request) {
        Booking booking = bookingService.recordPrepayment(id, request);
        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ConfirmBookingRequest request) {
        Booking booking = bookingService.confirmBooking(id, request);
        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<BookingResponse> startVisit(@PathVariable("id") UUID id) {
        Booking booking = bookingService.startVisit(id);
        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) CancelBookingRequest request) {
        Booking booking = bookingService.cancelBooking(id, request);
        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<BookingResponse> completeBooking(@PathVariable("id") UUID id) {
        Booking booking = bookingService.completeBooking(id);
        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<BookingResponse> noShowBooking(@PathVariable("id") UUID id) {
        Booking booking = bookingService.noShowBooking(id);
        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable("id") UUID id) {
        Booking booking = bookingService.getBooking(id);
        return ResponseEntity.ok(bookingMapper.toResponse(booking));
    }

    @GetMapping
    public ResponseEntity<Page<BookingResponse>> getBookings(
            @RequestParam(name = "clientId", required = false) UUID clientId,
            @RequestParam(name = "gameSlotId", required = false) UUID gameSlotId,
            @RequestParam(name = "status", required = false) BookingStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BookingResponse> page = bookingService.getBookings(clientId, gameSlotId, status, pageable)
                .map(bookingMapper::toResponse);
        return ResponseEntity.ok(page);
    }
}
