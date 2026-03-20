package com.pb.catalog.repository;

import com.pb.catalog.domain.entity.TariffIncludedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TariffIncludedItemRepository extends JpaRepository<TariffIncludedItem, UUID> {
    List<TariffIncludedItem> findByTariffId(UUID tariffId);
    boolean existsByTariffIdAndProductId(UUID tariffId, UUID productId);
}
