package com.pb.catalog.api.controller;

import com.pb.catalog.api.dto.request.CreateProductRequest;
import com.pb.catalog.api.dto.request.UpdateProductRequest;
import com.pb.catalog.api.dto.response.ProductResponse;
import com.pb.catalog.domain.enums.ProductCategory;
import com.pb.catalog.domain.enums.ProductSubcategory;
import com.pb.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest req) {
        return productService.createProduct(req);
    }

    @GetMapping
    public List<ProductResponse> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) ProductSubcategory subcategory) {
        return productService.getAllProducts(active, category, subcategory);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable("id") UUID id) {
        return productService.getProduct(id);
    }

    @PatchMapping("/{id}")
    public ProductResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductRequest req) {
        return productService.updateProduct(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("id") UUID id) {
        productService.deactivateProduct(id);
    }
}
