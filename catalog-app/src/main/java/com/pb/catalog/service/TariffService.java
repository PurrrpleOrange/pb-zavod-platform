package com.pb.catalog.service;

import com.pb.catalog.api.dto.request.AddTariffAddonRequest;
import com.pb.catalog.api.dto.request.AddTariffIncludedItemRequest;
import com.pb.catalog.api.dto.request.CreateTariffRequest;
import com.pb.catalog.api.dto.request.UpdateTariffRequest;
import com.pb.catalog.api.dto.response.*;
import com.pb.catalog.domain.entity.*;
import com.pb.catalog.exception.BusinessException;
import com.pb.catalog.exception.ConflictException;
import com.pb.catalog.exception.NotFoundException;
import com.pb.catalog.repository.TariffAddonRepository;
import com.pb.catalog.repository.TariffIncludedItemRepository;
import com.pb.catalog.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRepository tariffRepository;
    private final TariffIncludedItemRepository includedItemRepository;
    private final TariffAddonRepository addonRepository;
    private final GameTypeService gameTypeService;
    private final ProductService productService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public TariffResponse createTariff(CreateTariffRequest req) {
        gameTypeService.findOrThrow(req.getGameTypeId());
        Tariff tariff = new Tariff(
                UUID.randomUUID(),
                req.getGameTypeId(),
                req.getName(),
                req.getPricePerPlayer(),
                true
        );
        return toResponse(tariffRepository.save(tariff));
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> getAllTariffs(Boolean active, UUID gameTypeId) {
        List<Tariff> tariffs;
        if (active != null && gameTypeId != null) {
            tariffs = tariffRepository.findByActiveAndGameTypeId(active, gameTypeId);
        } else if (active != null) {
            tariffs = tariffRepository.findByActive(active);
        } else if (gameTypeId != null) {
            tariffs = tariffRepository.findByGameTypeId(gameTypeId);
        } else {
            tariffs = tariffRepository.findAll();
        }
        return tariffs.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TariffDetailResponse getTariffDetail(UUID id) {
        Tariff tariff = findOrThrow(id);

        List<TariffIncludedItem> includedItems = includedItemRepository.findByTariffId(id);
        List<TariffAddon> addons = addonRepository.findByTariffId(id);

        // Bulk-fetch product names
        Set<UUID> productIds = new HashSet<>();
        includedItems.forEach(i -> productIds.add(i.getProductId()));
        addons.forEach(a -> productIds.add(a.getProductId()));

        Map<UUID, String> productNames = productIds.isEmpty()
                ? Map.of()
                : productService.findAllByIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, Product::getName));

        return TariffDetailResponse.builder()
                .id(tariff.getId())
                .gameTypeId(tariff.getGameTypeId())
                .name(tariff.getName())
                .pricePerPlayer(tariff.getPricePerPlayer())
                .active(tariff.isActive())
                .includedItems(includedItems.stream()
                        .map(i -> TariffIncludedItemResponse.builder()
                                .id(i.getId())
                                .productId(i.getProductId())
                                .productName(productNames.getOrDefault(i.getProductId(), null))
                                .quantityPerPlayer(i.getQuantityPerPlayer())
                                .quantityFixed(i.getQuantityFixed())
                                .build())
                        .toList())
                .addons(addons.stream()
                        .map(a -> TariffAddonResponse.builder()
                                .id(a.getId())
                                .productId(a.getProductId())
                                .productName(productNames.getOrDefault(a.getProductId(), null))
                                .price(a.getPrice())
                                .maxQtyPerPlayer(a.getMaxQtyPerPlayer())
                                .maxQtyPerBooking(a.getMaxQtyPerBooking())
                                .build())
                        .toList())
                .build();
    }

    @Transactional
    public TariffResponse updateTariff(UUID id, UpdateTariffRequest req) {
        Tariff tariff = findOrThrow(id);
        if (req.getName() != null) tariff.setName(req.getName());
        if (req.getPricePerPlayer() != null) tariff.setPricePerPlayer(req.getPricePerPlayer());
        if (req.getActive() != null) tariff.setActive(req.getActive());
        return toResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public void deactivateTariff(UUID id) {
        Tariff tariff = findOrThrow(id);
        tariff.setActive(false);
        tariffRepository.save(tariff);
    }

    // ── INCLUDED ITEMS ───────────────────────────────────────────────────────

    @Transactional
    public TariffIncludedItemResponse addIncludedItem(UUID tariffId, AddTariffIncludedItemRequest req) {
        Tariff tariff = findActiveOrThrow(tariffId);
        productService.findActiveOrThrow(req.getProductId());

        if (includedItemRepository.existsByTariffIdAndProductId(tariffId, req.getProductId())) {
            throw BusinessException.of("TARIFF_INCLUDED_ITEM_DUPLICATE",
                    "Product already included in this tariff",
                    Map.of("tariffId", tariffId, "productId", req.getProductId()));
        }

        TariffIncludedItem item = new TariffIncludedItem(
                UUID.randomUUID(),
                tariffId,
                req.getProductId(),
                req.getQuantityPerPlayer(),
                req.getQuantityFixed()
        );
        item = includedItemRepository.save(item);

        Product product = productService.findOrThrow(req.getProductId());
        return TariffIncludedItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(product.getName())
                .quantityPerPlayer(item.getQuantityPerPlayer())
                .quantityFixed(item.getQuantityFixed())
                .build();
    }

    @Transactional
    public void removeIncludedItem(UUID tariffId, UUID itemId) {
        findOrThrow(tariffId);
        TariffIncludedItem item = includedItemRepository.findById(itemId)
                .orElseThrow(() -> NotFoundException.of("TARIFF_INCLUDED_ITEM_NOT_FOUND",
                        "Included item not found", Map.of("itemId", itemId)));
        if (!item.getTariffId().equals(tariffId)) {
            throw BusinessException.of("TARIFF_ITEM_MISMATCH",
                    "Item does not belong to this tariff",
                    Map.of("tariffId", tariffId, "itemId", itemId));
        }
        includedItemRepository.delete(item);
    }

    // ── ADDONS ───────────────────────────────────────────────────────────────

    @Transactional
    public TariffAddonResponse addAddon(UUID tariffId, AddTariffAddonRequest req) {
        Tariff tariff = findActiveOrThrow(tariffId);
        productService.findActiveOrThrow(req.getProductId());

        if (addonRepository.existsByTariffIdAndProductId(tariffId, req.getProductId())) {
            throw BusinessException.of("TARIFF_ADDON_DUPLICATE",
                    "Product already added as addon to this tariff",
                    Map.of("tariffId", tariffId, "productId", req.getProductId()));
        }

        TariffAddon addon = new TariffAddon(
                UUID.randomUUID(),
                tariffId,
                req.getProductId(),
                req.getPrice(),
                req.getMaxQtyPerPlayer(),
                req.getMaxQtyPerBooking()
        );
        addon = addonRepository.save(addon);

        Product product = productService.findOrThrow(req.getProductId());
        return TariffAddonResponse.builder()
                .id(addon.getId())
                .productId(addon.getProductId())
                .productName(product.getName())
                .price(addon.getPrice())
                .maxQtyPerPlayer(addon.getMaxQtyPerPlayer())
                .maxQtyPerBooking(addon.getMaxQtyPerBooking())
                .build();
    }

    @Transactional
    public void removeAddon(UUID tariffId, UUID addonId) {
        findOrThrow(tariffId);
        TariffAddon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> NotFoundException.of("TARIFF_ADDON_NOT_FOUND",
                        "Addon not found", Map.of("addonId", addonId)));
        if (!addon.getTariffId().equals(tariffId)) {
            throw BusinessException.of("TARIFF_ADDON_MISMATCH",
                    "Addon does not belong to this tariff",
                    Map.of("tariffId", tariffId, "addonId", addonId));
        }
        addonRepository.delete(addon);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    public Tariff findOrThrow(UUID id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("CATALOG_TARIFF_NOT_FOUND",
                        "Tariff not found", Map.of("tariffId", id)));
    }

    private Tariff findActiveOrThrow(UUID id) {
        Tariff tariff = findOrThrow(id);
        if (!tariff.isActive()) {
            throw ConflictException.of("CATALOG_TARIFF_INACTIVE",
                    "Tariff is inactive", Map.of("tariffId", id));
        }
        return tariff;
    }

    private TariffResponse toResponse(Tariff t) {
        return TariffResponse.builder()
                .id(t.getId())
                .gameTypeId(t.getGameTypeId())
                .name(t.getName())
                .pricePerPlayer(t.getPricePerPlayer())
                .active(t.isActive())
                .build();
    }
}
