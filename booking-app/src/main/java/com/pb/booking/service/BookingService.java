package com.pb.booking.service;

import com.pb.booking.api.dto.request.CancelBookingRequest;
import com.pb.booking.api.dto.request.ConfirmBookingRequest;
import com.pb.booking.api.dto.request.RecordPrepaymentRequest;
import com.pb.booking.api.dto.request.SubmitBookingRequest;
import com.pb.booking.api.dto.request.UpdateBookingRequest;
import com.pb.booking.client.SchedulingClient;
import com.pb.booking.client.dto.HoldSlotResponse;
import com.pb.booking.client.dto.HoldZoneResponse;
import com.pb.booking.config.BookingProperties;
import com.pb.booking.domain.entity.Booking;
import com.pb.booking.domain.entity.Client;
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

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final SchedulingClient schedulingClient;
    private final BookingProperties bookingProperties;

    @Transactional
    public Booking submitBooking(SubmitBookingRequest request) {
        // Find or create client: by phone first, then email, then create new
        Client client = null;
        if (request.getClientPhone() != null) {
            client = clientRepository.findByPhone(request.getClientPhone()).orElse(null);
        }
        if (client == null && request.getClientEmail() != null) {
            client = clientRepository.findByEmail(request.getClientEmail()).orElse(null);
        }
        if (client == null) {
            client = Client.builder()
                    .name(request.getClientName())
                    .phone(request.getClientPhone())
                    .email(request.getClientEmail())
                    .build();
            client = clientRepository.save(client);
        }

        Instant holdExpiresAt = Instant.now().plusSeconds(bookingProperties.getHoldExpiryMinutes() * 60L);

        Booking booking = Booking.builder()
                .clientId(client.getId())
                .gameSlotId(request.getGameSlotId())
                .tariffId(request.getTariffId())
                .playersCount(request.getPlayersCount())
                .totalPriceSnapshot(request.getTotalPriceSnapshot())
                .desiredDate(request.getDesiredDate())
                .extraEquipmentCount(request.getExtraEquipmentCount())
                .status(BookingStatus.HOLD)
                .holdExpiresAt(holdExpiresAt)
                .build();

        booking = bookingRepository.saveAndFlush(booking);

        // Hold slot only if gameSlotId provided (Type B booking)
        if (request.getGameSlotId() != null) {
            HoldSlotResponse holdResponse;
            try {
                holdResponse = schedulingClient.holdSlot(request.getGameSlotId(), booking.getId());
            } catch (BookingException e) {
                bookingRepository.delete(booking);
                throw e;
            } catch (Exception e) {
                bookingRepository.delete(booking);
                log.error("Failed to hold slot for booking {}", booking.getId(), e);
                throw new BookingException(ErrorCode.BOOKING_SLOT_HOLD_FAILED, "Scheduling service unavailable");
            }

            booking.setSlotReservationId(holdResponse.getSlotReservationId());

            // Hold zone only if zoneId provided
            if (request.getZoneId() != null) {
                try {
                    HoldZoneResponse zoneResponse = schedulingClient.holdZone(
                            request.getZoneId(), booking.getId(), holdResponse.getSlotReservationId());
                    booking.setZoneReservationId(zoneResponse.getZoneReservationId());
                } catch (BookingException e) {
                    schedulingClient.cancelSlotHold(holdResponse.getSlotReservationId(), booking.getId());
                    bookingRepository.delete(booking);
                    throw e;
                } catch (Exception e) {
                    schedulingClient.cancelSlotHold(holdResponse.getSlotReservationId(), booking.getId());
                    bookingRepository.delete(booking);
                    log.error("Failed to hold zone for booking {}", booking.getId(), e);
                    throw new BookingException(ErrorCode.BOOKING_ZONE_HOLD_FAILED, "Scheduling service unavailable");
                }
            }
        }

        booking = bookingRepository.save(booking);
        log.info("Booking submitted: id={}, status={}", booking.getId(), booking.getStatus());
        return booking;
    }

    @Transactional
    public Booking recordPrepayment(UUID bookingId, RecordPrepaymentRequest request) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() == BookingStatus.PREPAID) {
            return booking; // idempotent
        }

        if (booking.getStatus() != BookingStatus.HOLD) {
            throw new BookingException(ErrorCode.BOOKING_INVALID_STATE,
                    "Cannot record prepayment for booking in status " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.PREPAID);
        booking.setPrepaidAmount(request.getPrepaidAmount());
        booking.setHoldExpiresAt(null); // no longer subject to auto-expiry

        // Promote slot hold to CONFIRMED in scheduling-app
        if (booking.getSlotReservationId() != null) {
            schedulingClient.confirmSlotHold(booking.getSlotReservationId(), bookingId);
        }

        // Promote zone hold to ACTIVE in scheduling-app
        if (booking.getZoneReservationId() != null) {
            schedulingClient.confirmZoneHold(booking.getZoneReservationId(), bookingId);
        }

        booking = bookingRepository.save(booking);
        log.info("Prepayment recorded: id={}, amount={}", bookingId, request.getPrepaidAmount());
        return booking;
    }

    @Transactional
    public Booking confirmBooking(UUID bookingId, ConfirmBookingRequest request) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return booking; // idempotent
        }

        if (booking.getStatus() != BookingStatus.PREPAID) {
            throw new BookingException(ErrorCode.BOOKING_INVALID_STATE,
                    "Cannot confirm booking in status " + booking.getStatus());
        }

        // Patch missing fields from request
        if (request != null) {
            if (booking.getGameSlotId() == null && request.getGameSlotId() != null) {
                booking.setGameSlotId(request.getGameSlotId());
            }
            if (booking.getTariffId() == null && request.getTariffId() != null) {
                booking.setTariffId(request.getTariffId());
            }
            if (booking.getTotalPriceSnapshot() == null && request.getTotalPriceSnapshot() != null) {
                booking.setTotalPriceSnapshot(request.getTotalPriceSnapshot());
            }
            if (booking.getExtraEquipmentCount() == null && request.getExtraEquipmentCount() != null) {
                booking.setExtraEquipmentCount(request.getExtraEquipmentCount());
            }
            if (request.getAdminNotes() != null) {
                booking.setAdminNotes(request.getAdminNotes());
            }
        }

        // Type A: slot not yet reserved — hold + confirm now
        if (booking.getSlotReservationId() == null && booking.getGameSlotId() != null) {
            HoldSlotResponse holdResponse = schedulingClient.holdSlot(booking.getGameSlotId(), bookingId);
            booking.setSlotReservationId(holdResponse.getSlotReservationId());
            schedulingClient.confirmSlotHold(booking.getSlotReservationId(), bookingId);
        }

        // Type A: zone not yet reserved — hold + confirm now (requires slotReservationId)
        UUID zoneId = request != null ? request.getZoneId() : null;
        if (booking.getZoneReservationId() == null && zoneId != null && booking.getSlotReservationId() != null) {
            HoldZoneResponse zoneResponse = schedulingClient.holdZone(zoneId, bookingId, booking.getSlotReservationId());
            booking.setZoneReservationId(zoneResponse.getZoneReservationId());
            schedulingClient.confirmZoneHold(booking.getZoneReservationId(), bookingId);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        log.info("Booking confirmed: id={}", bookingId);
        return booking;
    }

    @Transactional
    public Booking startVisit(UUID bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            return booking; // idempotent
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException(ErrorCode.BOOKING_INVALID_STATE,
                    "Cannot start visit for booking in status " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking = bookingRepository.save(booking);
        log.info("Booking started: id={}", bookingId);
        return booking;
    }

    @Transactional
    public Booking completeBooking(UUID bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return booking;
        }

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BookingException(ErrorCode.BOOKING_INVALID_STATE,
                    "Cannot complete booking in status " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        releaseSchedulingResources(booking, true);

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

        releaseSchedulingResources(booking, false);

        log.info("Booking marked as no-show: id={}", bookingId);
        return booking;
    }

    @Transactional
    public Booking cancelBooking(UUID bookingId, CancelBookingRequest request) {
        Booking booking = findBookingOrThrow(bookingId);

        // Idempotent: already in a cancelled state
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.CANCELLED_UNPAID) {
            return booking;
        }

        BookingStatus newStatus;
        if (booking.getStatus() == BookingStatus.HOLD) {
            newStatus = BookingStatus.CANCELLED_UNPAID;
        } else if (booking.getStatus() == BookingStatus.PREPAID
                || booking.getStatus() == BookingStatus.CONFIRMED) {
            newStatus = BookingStatus.CANCELLED;
        } else {
            throw new BookingException(ErrorCode.BOOKING_INVALID_STATE,
                    "Cannot cancel booking in status " + booking.getStatus());
        }

        releaseSchedulingResources(booking, false);

        booking.setStatus(newStatus);
        if (request != null && request.getReason() != null) {
            booking.setCancelReason(request.getReason());
        }

        booking = bookingRepository.save(booking);
        log.info("Booking cancelled: id={}, status={}, reason={}", bookingId, newStatus, booking.getCancelReason());
        return booking;
    }

    @Transactional
    public Booking updateBooking(UUID bookingId, UpdateBookingRequest request) {
        Booking booking = findBookingOrThrow(bookingId);

        if (request.getPlayersCount() != null) booking.setPlayersCount(request.getPlayersCount());
        if (request.getDesiredDate() != null) booking.setDesiredDate(request.getDesiredDate());
        if (request.getTariffId() != null) booking.setTariffId(request.getTariffId());
        if (request.getTotalPriceSnapshot() != null) booking.setTotalPriceSnapshot(request.getTotalPriceSnapshot());
        if (request.getExtraEquipmentCount() != null) booking.setExtraEquipmentCount(request.getExtraEquipmentCount());
        if (request.getAdminNotes() != null) booking.setAdminNotes(request.getAdminNotes());

        return bookingRepository.save(booking);
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

    void releaseSchedulingResources(Booking booking, boolean finishZoneChain) {
        if (booking.getSlotReservationId() != null) {
            schedulingClient.cancelSlotHold(booking.getSlotReservationId(), booking.getId());
        }
        if (booking.getZoneReservationId() != null) {
            if (finishZoneChain) {
                schedulingClient.finishZoneChain(booking.getZoneReservationId(), booking.getId());
            } else {
                schedulingClient.cancelZoneHold(booking.getZoneReservationId(), booking.getId());
            }
        }
    }

    private Booking findBookingOrThrow(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException(ErrorCode.BOOKING_NOT_FOUND));
    }
}
