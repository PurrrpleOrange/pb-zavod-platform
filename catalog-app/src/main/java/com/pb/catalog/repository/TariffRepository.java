package com.pb.catalog.repository;

import com.pb.catalog.domain.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TariffRepository extends JpaRepository<Tariff, UUID> {
    List<Tariff> findByActive(boolean active);
    List<Tariff> findByGameTypeId(UUID gameTypeId);
    List<Tariff> findByActiveAndGameTypeId(boolean active, UUID gameTypeId);
}
