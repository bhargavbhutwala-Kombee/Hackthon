package com.kombee.orderly.repository;

import com.kombee.orderly.entity.Order;
import com.kombee.orderly.entity.Order.OrderStatus;
import com.kombee.orderly.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUser(User user, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.user = :user AND " +
           "(:status IS NULL OR o.status = :status)")
    Page<Order> findByUserAndStatus(@Param("user") User user, @Param("status") OrderStatus status, Pageable pageable);
}
