package com.storephone.dao;

import com.storephone.dto.PageResult;
import com.storephone.model.Order;
import com.storephone.model.OrderItem;
import com.storephone.model.OrderStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface OrderDAO {
    Order insertOrder(Connection conn, Order order);

    void insertOrderItems(Connection conn, Long orderId, List<OrderItem> items);

    Optional<Order> findById(Long orderId);

    Optional<Order> findByIdForUpdate(Connection conn, Long orderId);

    List<OrderItem> findItemsByOrderId(Long orderId);

    List<Order> findByUserId(Long userId);

    PageResult<Order> findPage(OrderStatus statusFilter, int page, int pageSize);

    void updateStatus(Connection conn, Long orderId, OrderStatus newStatus);

    long countAll();

    BigDecimal sumCompletedRevenue();
}
