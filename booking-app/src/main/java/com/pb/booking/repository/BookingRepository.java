package com.pb.booking.repository;

import com.pb.booking.domain.entity.Booking;
import com.pb.booking.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByClientId(UUID clientId, Pageable pageable);

    Page<Booking> findByGameSlotId(UUID gameSlotId, Pageable pageable);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    List<Booking> findByClientIdAndStatusIn(UUID clientId, List<BookingStatus> statuses);

    List<Booking> findByStatusAndHoldExpiresAtLessThanEqual(BookingStatus status, Instant now);
}
