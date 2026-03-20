package com.pb.catalog.service;

import com.pb.catalog.api.dto.request.CreateGameTypeRequest;
import com.pb.catalog.api.dto.request.UpdateGameTypeRequest;
import com.pb.catalog.api.dto.response.GameTypeResponse;
import com.pb.catalog.domain.entity.GameType;
import com.pb.catalog.exception.BusinessException;
import com.pb.catalog.exception.NotFoundException;
import com.pb.catalog.repository.GameTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameTypeService {

    private final GameTypeRepository gameTypeRepository;

    @Transactional
    public GameTypeResponse createGameType(CreateGameTypeRequest req) {
        if (gameTypeRepository.existsByCode(req.getCode())) {
            throw BusinessException.of("GAME_TYPE_CODE_DUPLICATE",
                    "Game type with code '" + req.getCode() + "' already exists");
        }
        GameType gameType = new GameType(UUID.randomUUID(), req.getName(), req.getCode());
        return toResponse(gameTypeRepository.save(gameType));
    }

    @Transactional(readOnly = true)
    public List<GameTypeResponse> getAllGameTypes() {
        return gameTypeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GameTypeResponse getGameType(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public GameTypeResponse updateGameType(UUID id, UpdateGameTypeRequest req) {
        GameType gameType = findOrThrow(id);
        if (req.getName() != null) gameType.setName(req.getName());
        return toResponse(gameTypeRepository.save(gameType));
    }

    public GameType findOrThrow(UUID id) {
        return gameTypeRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("CATALOG_GAME_TYPE_NOT_FOUND",
                        "Game type not found", Map.of("gameTypeId", id)));
    }

    private GameTypeResponse toResponse(GameType gt) {
        return GameTypeResponse.builder()
                .id(gt.getId())
                .code(gt.getCode())
                .name(gt.getName())
                .build();
    }
}
