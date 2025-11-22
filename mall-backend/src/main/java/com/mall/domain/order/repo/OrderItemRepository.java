package com.mall.domain.order.repo;

import com.mall.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findAllByOrderIdOrderByIdAsc(Long orderId);
}
