package com.pb.scheduling.repository;

import com.pb.scheduling.domain.entity.ZoneReservation;
import com.pb.scheduling.domain.enums.ZoneReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneReservationRepository extends JpaRepository<ZoneReservation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ZoneReservation r where r.id = :id")
    Optional<ZoneReservation> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Пересечения по зоне на интервал [startTime, endTime).
     * Учитываем:
     * - ACTIVE всегда
     * - HOLD только если expiresAt > now
     * Исключаем запись excludeId (можно null).
     */
    @Query("""
        select count(r)
        from ZoneReservation r
        where r.zoneId = :zoneId
          and r.startTime < :endTime
          and r.endTime > :startTime
          and (:excludeId is null or r.id <> :excludeId)
          and (
                r.status = com.pb.scheduling.domain.enums.ZoneReservationStatus.ACTIVE
                or (r.status = com.pb.scheduling.domain.enums.ZoneReservationStatus.HOLD and r.expiresAt > :now)
              )
        """)
    long countOverlaps(@Param("zoneId") UUID zoneId,
                       @Param("startTime") OffsetDateTime startTime,
                       @Param("endTime") OffsetDateTime endTime,
                       @Param("now") OffsetDateTime now,
                       @Param("excludeId") UUID excludeId);

    @Query("""
        select r from ZoneReservation r
        where (:zoneId is null or r.zoneId = :zoneId)
          and (:bookingId is null or r.bookingId = :bookingId)
          and (:status is null or r.status = :status)
        order by r.createdAt desc
        """)
    List<ZoneReservation> findAllFiltered(@Param("zoneId") UUID zoneId,
                                          @Param("bookingId") UUID bookingId,
                                          @Param("status") ZoneReservationStatus status);

    @Modifying
    @Query("""
    update ZoneReservation r
    set r.status = com.pb.scheduling.domain.enums.ZoneReservationStatus.CANCELLED,
        r.expiresAt = null
    where r.status = com.pb.scheduling.domain.enums.ZoneReservationStatus.HOLD
    and r.expiresAt <= :now
""")
    int cancelExpiredHolds(@Param("now") OffsetDateTime now);

    List<ZoneReservation> findByBookingIdAndZoneId(UUID bookingId, UUID zoneId);
}
