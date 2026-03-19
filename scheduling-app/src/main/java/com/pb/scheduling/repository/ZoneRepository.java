package com.pb.scheduling.repository;

import com.pb.scheduling.domain.entity.Zone;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select z from Zone z where z.id = :id")
    Optional<Zone> findByIdForUpdate(@Param("id") UUID id);

    List<Zone> findByActive(boolean active);

    boolean existsByCode(String code);
}
