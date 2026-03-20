package com.pb.catalog.service;

import com.pb.catalog.api.dto.request.CreateProductRequest;
import com.pb.catalog.api.dto.request.UpdateProductRequest;
import com.pb.catalog.api.dto.response.ProductResponse;
import com.pb.catalog.domain.entity.Product;
import com.pb.catalog.domain.enums.ProductCategory;
import com.pb.catalog.domain.enums.ProductSubcategory;
import com.pb.catalog.exception.BusinessException;
import com.pb.catalog.exception.ConflictException;
import com.pb.catalog.exception.NotFoundException;
import com.pb.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req) {
        if (productRepository.existsByCode(req.getCode())) {
            throw BusinessException.of("PRODUCT_CODE_DUPLICATE",
                    "Product with code '" + req.getCode() + "' already exists");
        }
        Product product = new Product(
                UUID.randomUUID(),
                req.getCode(),
                req.getName(),
                req.getCategory(),
                req.getSubcategory(),
                req.getUnit(),
                req.getCurrentPrice(),
                true
        );
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(Boolean active, ProductCategory category, ProductSubcategory subcategory) {
        List<Product> products;
        if (active != null && category != null && subcategory != null) {
            products = productRepository.findByActiveAndCategoryAndSubcategory(active, category, subcategory);
        } else if (active != null && category != null) {
            products = productRepository.findByActiveAndCategory(active, category);
        } else if (active != null && subcategory != null) {
            products = productRepository.findByActiveAndSubcategory(active, subcategory);
        } else if (category != null && subcategory != null) {
            products = productRepository.findByCategoryAndSubcategory(category, subcategory);
        } else if (active != null) {
            products = productRepository.findByActive(active);
        } else if (category != null) {
            products = productRepository.findByCategory(category);
        } else if (subcategory != null) {
            products = productRepository.findBySubcategory(subcategory);
        } else {
            products = productRepository.findAll();
        }
        return products.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest req) {
        Product product = findOrThrow(id);
        if (req.getName() != null) product.setName(req.getName());
        if (req.getCategory() != null) product.setCategory(req.getCategory());
        if (req.getSubcategory() != null) product.setSubcategory(req.getSubcategory());
        if (req.getUnit() != null) product.setUnit(req.getUnit());
        if (req.getCurrentPrice() != null) product.setCurrentPrice(req.getCurrentPrice());
        if (req.getActive() != null) product.setActive(req.getActive());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deactivateProduct(UUID id) {
        Product product = findOrThrow(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public Product findOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("CATALOG_PRODUCT_NOT_FOUND",
                        "Product not found", Map.of("productId", id)));
    }

    public Product findActiveOrThrow(UUID id) {
        Product product = findOrThrow(id);
        if (!product.isActive()) {
            throw ConflictException.of("CATALOG_PRODUCT_INACTIVE",
                    "Product is inactive", Map.of("productId", id));
        }
        return product;
    }

    public List<Product> findAllByIds(Set<UUID> ids) {
        return productRepository.findAllById(ids);
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .category(p.getCategory())
                .subcategory(p.getSubcategory())
                .unit(p.getUnit())
                .currentPrice(p.getCurrentPrice())
                .active(p.isActive())
                .build();
    }
}
