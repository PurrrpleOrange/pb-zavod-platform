package com.pb.catalog.repository;

import com.pb.catalog.domain.entity.GameType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameTypeRepository extends JpaRepository<GameType, UUID> {
    boolean existsByCode(String code);
}
