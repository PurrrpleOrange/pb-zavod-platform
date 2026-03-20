package com.pb.catalog.repository;

import com.pb.catalog.domain.entity.TariffAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TariffAddonRepository extends JpaRepository<TariffAddon, UUID> {
    List<TariffAddon> findByTariffId(UUID tariffId);
    boolean existsByTariffIdAndProductId(UUID tariffId, UUID productId);
}
