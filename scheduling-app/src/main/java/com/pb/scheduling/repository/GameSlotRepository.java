package com.pb.scheduling.repository;

import com.pb.scheduling.domain.entity.GameSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSlotRepository extends JpaRepository<GameSlot, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GameSlot s where s.id = :id")
    Optional<GameSlot> findByIdForUpdate(@Param("id") UUID id);

}
