package com.pb.catalog.api.controller;

import com.pb.catalog.api.dto.request.CreateGameTypeRequest;
import com.pb.catalog.api.dto.request.UpdateGameTypeRequest;
import com.pb.catalog.api.dto.response.GameTypeResponse;
import com.pb.catalog.service.GameTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/game-types")
@RequiredArgsConstructor
public class GameTypeController {

    private final GameTypeService gameTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameTypeResponse create(@Valid @RequestBody CreateGameTypeRequest req) {
        return gameTypeService.createGameType(req);
    }

    @GetMapping
    public List<GameTypeResponse> getAll() {
        return gameTypeService.getAllGameTypes();
    }

    @GetMapping("/{id}")
    public GameTypeResponse getById(@PathVariable("id") UUID id) {
        return gameTypeService.getGameType(id);
    }

    @PatchMapping("/{id}")
    public GameTypeResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateGameTypeRequest req) {
        return gameTypeService.updateGameType(id, req);
    }
}
