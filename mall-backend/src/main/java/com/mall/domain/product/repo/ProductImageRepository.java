package com.mall.domain.product.repo;

import com.mall.domain.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage,Long> {
    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);
}
