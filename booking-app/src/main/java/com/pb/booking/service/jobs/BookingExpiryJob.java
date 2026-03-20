package com.pb.booking.service.jobs;

import com.pb.booking.client.SchedulingClient;
import com.pb.booking.domain.entity.Booking;
import com.pb.booking.domain.enums.BookingStatus;
import com.pb.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryJob {

    private final BookingRepository bookingRepository;
    private final SchedulingClient schedulingClient;

    @Scheduled(fixedDelayString = "${pb.booking.expiry-check-interval-ms:60000}")
    @Transactional
    public void expireUnpaidHolds() {
        List<Booking> expiredHolds = bookingRepository
                .findByStatusAndHoldExpiresAtLessThanEqual(BookingStatus.HOLD, Instant.now());

        if (expiredHolds.isEmpty()) {
            return;
        }

        log.info("Expiring {} unpaid HOLD bookings", expiredHolds.size());

        for (Booking booking : expiredHolds) {
            try {
                releaseSchedulingResources(booking);
                booking.setStatus(BookingStatus.CANCELLED_UNPAID);
                bookingRepository.save(booking);
                log.info("Auto-expired booking: id={}", booking.getId());
            } catch (Exception e) {
                log.error("Failed to auto-expire booking: id={}", booking.getId(), e);
            }
        }
    }

    private void releaseSchedulingResources(Booking booking) {
        if (booking.getSlotReservationId() != null) {
            schedulingClient.cancelSlotHold(booking.getSlotReservationId(), booking.getId());
        }
        if (booking.getZoneReservationId() != null) {
            schedulingClient.cancelZoneHold(booking.getZoneReservationId(), booking.getId());
        }
    }
}
