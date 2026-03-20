package com.pb.catalog.repository;

import com.pb.catalog.domain.entity.Product;
import com.pb.catalog.domain.enums.ProductCategory;
import com.pb.catalog.domain.enums.ProductSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsByCode(String code);
    List<Product> findByActive(boolean active);
    List<Product> findByCategory(ProductCategory category);
    List<Product> findByActiveAndCategory(boolean active, ProductCategory category);
    List<Product> findBySubcategory(ProductSubcategory subcategory);
    List<Product> findByActiveAndSubcategory(boolean active, ProductSubcategory subcategory);
    List<Product> findByCategoryAndSubcategory(ProductCategory category, ProductSubcategory subcategory);
    List<Product> findByActiveAndCategoryAndSubcategory(boolean active, ProductCategory category, ProductSubcategory subcategory);
}
