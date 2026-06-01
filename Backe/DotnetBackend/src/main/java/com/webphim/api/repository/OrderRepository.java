package com.webphim.api.repository;

import com.webphim.api.entity.Order;
import com.webphim.api.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByStatusAndFinalAmountAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(OrderStatus status, BigDecimal amount, LocalDateTime cutoff);
    List<Order> findByStatusAndExpiredAtBefore(OrderStatus status, LocalDateTime now);
    List<Order> findByUserUserIdAndStatusOrderByCreatedAtDesc(Integer userId, OrderStatus status);

    @Query("select o from Order o left join fetch o.user where o.status = com.webphim.api.entity.OrderStatus.paid and (:fromDate is null or o.updatedAt >= :fromDate) and (:toDate is null or o.updatedAt <= :toDate) and (:minAmount is null or o.finalAmount >= :minAmount) and (:maxAmount is null or o.finalAmount <= :maxAmount) and (:search is null or lower(o.orderCode) like lower(concat('%', :search, '%')) or lower(o.user.fullName) like lower(concat('%', :search, '%')) or lower(o.user.email) like lower(concat('%', :search, '%'))) order by o.updatedAt desc")
    List<Order> searchPaidInvoices(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, @Param("search") String search, @Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);

    @Query("select coalesce(sum(o.finalAmount), 0) from Order o where o.status = com.webphim.api.entity.OrderStatus.paid and (:fromDate is null or o.createdAt >= :fromDate) and (:toDate is null or o.createdAt <= :toDate)")
    BigDecimal sumRevenue(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    @Query("select o from Order o where o.status = com.webphim.api.entity.OrderStatus.paid and (:fromDate is null or o.createdAt >= :fromDate) and (:toDate is null or o.createdAt <= :toDate) order by o.createdAt asc")
    List<Order> paidOrdersForRevenue(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);
}
