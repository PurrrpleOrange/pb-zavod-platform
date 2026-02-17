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

    @Query("""
        select count(r) from ZoneReservation r
        where r.zoneId = :zoneId
          and r.status in :statuses
          and (r.status <> com.pb.scheduling.domain.enums.ZoneReservationStatus.HOLD or r.createdAt is not null)
          and r.startTime < :endTime
          and r.endTime > :startTime
          and (:excludeId is null or r.id <> :excludeId)
    """)
    long countOverlaps(
            @Param("zoneId") UUID zoneId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("statuses") List<ZoneReservationStatus> statuses,
            @Param("excludeId") UUID excludeId
    );

    @Query("""
        select r from ZoneReservation r
        where r.zoneId = :zoneId
          and r.startTime < :to
          and r.endTime > :from
        order by r.startTime
    """)
    List<ZoneReservation> findInRange(@Param("zoneId") UUID zoneId,
                                      @Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to);
}

//Примечание: для HOLD зон по ТЗ нужен expires_at,в схеме его нет.
//Поэтому в MVP я делаю HOLD без автоистечения (или ты добавишь expires_at). Если хочешь строго по ТЗ — скажи, дам миграцию + поля.