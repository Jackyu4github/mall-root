package com.mall.domain.product.repo;

import com.mall.domain.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory,Long> {
    List<ProductCategory> findByIsActiveTrueOrderBySortOrderAsc();
}
