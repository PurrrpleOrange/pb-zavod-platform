package com.pb.catalog.api.controller;

import com.pb.catalog.api.dto.request.AddTariffAddonRequest;
import com.pb.catalog.api.dto.request.AddTariffIncludedItemRequest;
import com.pb.catalog.api.dto.request.CreateTariffRequest;
import com.pb.catalog.api.dto.request.UpdateTariffRequest;
import com.pb.catalog.api.dto.response.TariffAddonResponse;
import com.pb.catalog.api.dto.response.TariffDetailResponse;
import com.pb.catalog.api.dto.response.TariffIncludedItemResponse;
import com.pb.catalog.api.dto.response.TariffResponse;
import com.pb.catalog.service.TariffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tariffs")
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TariffResponse create(@Valid @RequestBody CreateTariffRequest req) {
        return tariffService.createTariff(req);
    }

    @GetMapping
    public List<TariffResponse> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UUID gameTypeId) {
        return tariffService.getAllTariffs(active, gameTypeId);
    }

    @GetMapping("/{id}")
    public TariffDetailResponse getById(@PathVariable("id") UUID id) {
        return tariffService.getTariffDetail(id);
    }

    @PatchMapping("/{id}")
    public TariffResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTariffRequest req) {
        return tariffService.updateTariff(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("id") UUID id) {
        tariffService.deactivateTariff(id);
    }

    // ── INCLUDED ITEMS ───────────────────────────────────────────────────────

    @PostMapping("/{id}/included-items")
    @ResponseStatus(HttpStatus.CREATED)
    public TariffIncludedItemResponse addIncludedItem(
            @PathVariable("id") UUID tariffId,
            @Valid @RequestBody AddTariffIncludedItemRequest req) {
        return tariffService.addIncludedItem(tariffId, req);
    }

    @DeleteMapping("/{id}/included-items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeIncludedItem(
            @PathVariable("id") UUID tariffId,
            @PathVariable("itemId") UUID itemId) {
        tariffService.removeIncludedItem(tariffId, itemId);
    }

    // ── ADDONS ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/addons")
    @ResponseStatus(HttpStatus.CREATED)
    public TariffAddonResponse addAddon(
            @PathVariable("id") UUID tariffId,
            @Valid @RequestBody AddTariffAddonRequest req) {
        return tariffService.addAddon(tariffId, req);
    }

    @DeleteMapping("/{id}/addons/{addonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAddon(
            @PathVariable("id") UUID tariffId,
            @PathVariable("addonId") UUID addonId) {
        tariffService.removeAddon(tariffId, addonId);
    }
}
