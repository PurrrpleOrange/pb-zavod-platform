package com.pb.booking.service;

import com.pb.booking.api.dto.request.CancelBookingRequest;
import com.pb.booking.api.dto.request.CreateBookingRequest;
import com.pb.booking.client.SchedulingClient;
import com.pb.booking.client.dto.HoldSlotResponse;
import com.pb.booking.client.dto.HoldZoneResponse;
import com.pb.booking.domain.entity.Booking;
import com.pb.booking.domain.enums.BookingStatus;
import com.pb.booking.exception.BookingException;
import com.pb.booking.exception.ErrorCode;
import com.pb.booking.repository.BookingRepository;
import com.pb.booking.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final SchedulingClient schedulingClient;

    @Transactional
    public Booking createBooking(CreateBookingRequest request) {
        // Validate client exists
        clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new BookingException(ErrorCode.BOOKING_CLIENT_INVALID,
                        "Client not found: " + request.getClientId()));

        Booking booking = Booking.builder()
                .clientId(request.getClientId())
                .gameSlotId(request.getGameSlotId())
                .tariffId(request.getTariffId())
                .playersCount(request.getPlayersCount())
                .totalPriceSnapshot(request.getTotalPriceSnapshot())
                .status(BookingStatus.HOLD)
                .build();

        booking = bookingRepository.saveAndFlush(booking);

        // Hold slot in scheduling service
        HoldSlotResponse holdResponse;
        try {
            holdResponse = schedulingClient.holdSlot(request.getGameSlotId(), booking.getId());
        } catch (BookingException e) {
            bookingRepository.delete(booking);
            throw e;
        } catch (Exception e) {
            bookingRepository.delete(booking);
            log.error("Failed to hold slot for booking {}", booking.getId(), e);
            throw new BookingException(ErrorCode.BOOKING_SLOT_HOLD_FAILED,
                    "Scheduling service unavailable");
        }

        booking.setSlotReservationId(holdResponse.getSlotReservationId());

        // Optionally hold zone
        if (request.getZoneId() != null) {
            try {
                HoldZoneResponse zoneResponse = schedulingClient.holdZone(
                        request.getZoneId(), booking.getId(), holdResponse.getSlotReservationId());
                booking.setZoneReservationId(zoneResponse.getZoneReservationId());
            } catch (BookingException e) {
                // Rollback slot hold
                schedulingClient.cancelSlotHold(holdResponse.getSlotReservationId(), booking.getId());
                bookingRepository.delete(booking);
                throw e;
            } catch (Exception e) {
                schedulingClient.cancelSlotHold(holdResponse.getSlotReservationId(), booking.getId());
                bookingRepository.delete(booking);
                log.error("Failed to hold zone for booking {}", booking.getId(), e);
                throw new BookingException(ErrorCode.BOOKING_ZONE_HOLD_FAILED,
                        "Scheduling service unavailable");
            }
        }

        booking = bookingRepository.save(booking);
        log.info("Booking created: id={}, status={}", booking.getId(), booking.getStatus());
        return booking;
    }

    @Transactional
    public Booking confirmBooking(UUID bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        // Idempotent: already confirmed
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return booking;
        }

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new BookingException(ErrorCode.BOOKING_INVALID_STATE,
                    "Cannot confirm booking in status " + booking.getStatus());
        }

        // Confirm slot hold in scheduling
        schedulingClient.confirmSlotHold(booking.getSlotReservationId(), bookingId);

        // Confirm zone hold if present
        if (booking.getZoneReservationId() != null) {
            schedulingClient.confirmZoneHold(booking.getZoneReservationId(), bookingId);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        log.info("Booking confirmed: id={}", bookingId);
        return booking;
    }

    @Transactional
    public Booking cancelBooking(UUID bookingId, CancelBookingRequest request) {
        Booking booking = findBookingOrThrow(bookingId);

        // Idempotent: already cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }

        if (booking.getStatus() != BookingStatus.HOLD && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException(ErrorCode.BOOKING_INVALID_STATE,
                    "Cannot cancel booking in status " + booking.getStatus());
        }

        releaseSchedulingResources(booking);

        booking.setStatus(BookingStatus.CANCELLED);
        if (request != null && request.getReason() != null) {
            booking.setCancelReason(request.getReason());
        }

        booking = bookingRepository.save(booking);
        log.info("Booking cancelled: id={}, reason={}", bookingId, booking.getCancelReason());
        return booking;
    }

    @Transactional
    public Booking completeBooking(UUID bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return booking;
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException(ErrorCode.BOOKING_INVALID_STATE,
                    "Cannot complete booking in status " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        releaseSchedulingResources(booking);

        clientRepository.findById(booking.getClientId()).ifPresent(client -> {
            client.setVisitCount(client.getVisitCount() + 1);
            clientRepository.save(client);
        });

        log.info("Booking completed: id={}", bookingId);
        return booking;
    }

    @Transactional
    public Booking noShowBooking(UUID bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() == BookingStatus.NO_SHOW) {
            return booking;
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException(ErrorCode.BOOKING_INVALID_STATE,
                    "Cannot mark no-show for booking in status " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.NO_SHOW);
        booking = bookingRepository.save(booking);

        releaseSchedulingResources(booking);

        log.info("Booking marked as no-show: id={}", bookingId);
        return booking;
    }

    @Transactional(readOnly = true)
    public Booking getBooking(UUID bookingId) {
        return findBookingOrThrow(bookingId);
    }

    @Transactional(readOnly = true)
    public Page<Booking> getBookings(UUID clientId, UUID gameSlotId, BookingStatus status, Pageable pageable) {
        if (clientId != null) {
            return bookingRepository.findByClientId(clientId, pageable);
        }
        if (gameSlotId != null) {
            return bookingRepository.findByGameSlotId(gameSlotId, pageable);
        }
        if (status != null) {
            return bookingRepository.findByStatus(status, pageable);
        }
        return bookingRepository.findAll(pageable);
    }

    private void releaseSchedulingResources(Booking booking) {
        if (booking.getSlotReservationId() != null) {
            schedulingClient.cancelSlotHold(booking.getSlotReservationId(), booking.getId());
        }
        if (booking.getZoneReservationId() != null) {
            schedulingClient.cancelZoneHold(booking.getZoneReservationId(), booking.getId());
        }
    }

    private Booking findBookingOrThrow(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException(ErrorCode.BOOKING_NOT_FOUND));
    }
}
