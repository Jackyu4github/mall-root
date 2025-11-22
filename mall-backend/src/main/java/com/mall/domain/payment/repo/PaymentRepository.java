package com.mall.domain.payment.repo;

import com.mall.domain.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 1. 根据支付单号查（已有逻辑，例如回调用）
    Optional<Payment> findByPaymentNo(String paymentNo);

    // 2. 给 PaymentService.createPayment(...) 用：
    //    判断该订单是否已经有一条支付记录，避免重复创建
    Optional<Payment> findByOrderId(Long orderId);

    // 3. 给 OrderQueryService.getOrderDetail(...) 用：
    //    查这个订单的所有支付记录，按创建时间倒序
    List<Payment> findAllByOrderIdOrderByCreatedAtDesc(Long orderId);

    // 每单是否已有成功支付（用于统一下单前快速阻断）
    boolean existsByOrderIdAndStatus(Long orderId, Payment.Status status);

    // 幂等：第三方交易号是否已处理
    boolean existsByExternalTradeNo(String externalTradeNo);

    // 复用最近的 PENDING（防多点）——已有同义方法就跳过
    Optional<Payment> findTop1ByOrderIdAndMethodAndStatusOrderByCreatedAtDesc(
            Long orderId, Payment.Method method, Payment.Status status);

    // 回调强幂等：对同一 payment 行加锁
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentNo = :no")
    Optional<Payment> findByPaymentNoForUpdate(@Param("no") String paymentNo);
}
