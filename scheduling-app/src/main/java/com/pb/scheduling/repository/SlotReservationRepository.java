package com.pb.scheduling.repository;

import com.pb.scheduling.domain.entity.SlotReservation;
import com.pb.scheduling.domain.enums.SlotReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SlotReservationRepository extends JpaRepository<SlotReservation, UUID> {

    @Query("""
        select count(r) from SlotReservation r
        where r.gameSlotId = :slotId
          and (
               r.status = com.pb.scheduling.domain.enums.SlotReservationStatus.CONFIRMED
               or (r.status = com.pb.scheduling.domain.enums.SlotReservationStatus.HOLD and r.expiresAt > :now)
          )
    """)
    long countActive(@Param("slotId") UUID slotId, @Param("now") OffsetDateTime now);

    @Query("""
        select count(r) > 0 from SlotReservation r
        where r.gameSlotId = :slotId
          and r.exclusive = true
          and (
               r.status = com.pb.scheduling.domain.enums.SlotReservationStatus.CONFIRMED
               or (r.status = com.pb.scheduling.domain.enums.SlotReservationStatus.HOLD and r.expiresAt > :now)
          )
    """)
    boolean hasActiveExclusive(@Param("slotId") UUID slotId, @Param("now") OffsetDateTime now);

    Optional<SlotReservation> findByIdAndBookingId(UUID id, UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SlotReservation r where r.id = :id and r.bookingId = :bookingId")
    Optional<SlotReservation> findByIdAndBookingIdForUpdate(@Param("id") UUID id, @Param("bookingId") UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SlotReservation r where r.id = :id")
    Optional<SlotReservation> findByIdLocked(@Param("id")UUID id);

    @Modifying
    @Query("""
    update SlotReservation r
    set r.status = com.pb.scheduling.domain.enums.SlotReservationStatus.CANCELLED,
        r.expiresAt = null
    where r.status = com.pb.scheduling.domain.enums.SlotReservationStatus.HOLD
    and r.expiresAt <= :now
""")
    int cancelExpiredHolds(@Param("now") OffsetDateTime now);
}
