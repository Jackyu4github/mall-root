package com.mall.domain.product.repo;

import com.mall.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product,Long> {
    List<Product> findByCategoryIdAndIsOnShelfTrue(Long categoryId);
    List<Product> findByIsOnShelfTrue();

    @Query("""
        SELECT p FROM Product p
        WHERE (:keyword IS NULL OR :keyword = '' 
               OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.productSubtitle) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:onShelf IS NULL OR p.isOnShelf = :onShelf)
          AND (:saleMode IS NULL OR p.saleMode = :saleMode)
        ORDER BY p.updatedAt DESC
        """)
    Page<Product> pageAdminProducts(Pageable pageable,
                                    @Param("keyword") String keyword,
                                    @Param("categoryId") Long categoryId,
                                    @Param("onShelf") Boolean onShelf,
                                    @Param("saleMode") Product.SaleMode saleMode);

    List<Product> findByIdIn(List<Long> ids);

}
