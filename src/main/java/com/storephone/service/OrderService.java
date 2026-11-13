package com.storephone.service;

import com.storephone.config.ConnectionFactory;
import com.storephone.dao.OrderDAO;
import com.storephone.dao.ProductDAO;
import com.storephone.dto.OrderDetailView;
import com.storephone.dto.PageResult;
import com.storephone.dto.UserSession;
import com.storephone.exception.AuthorizationException;
import com.storephone.exception.DataAccessException;
import com.storephone.exception.ResourceNotFoundException;
import com.storephone.exception.ValidationException;
import com.storephone.model.Order;
import com.storephone.model.OrderItem;
import com.storephone.model.OrderStatus;
import com.storephone.security.AppSession;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class OrderService {
    private final ConnectionFactory connectionFactory;
    private final OrderDAO orderDAO;
    private final ProductDAO productDAO;
    private final AppSession appSession;
    private final AuthorizationService authorizationService;

    public OrderService(
            ConnectionFactory connectionFactory,
            OrderDAO orderDAO,
            ProductDAO productDAO,
            AppSession appSession,
            AuthorizationService authorizationService
    ) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
        this.orderDAO = Objects.requireNonNull(orderDAO);
        this.productDAO = Objects.requireNonNull(productDAO);
        this.appSession = Objects.requireNonNull(appSession);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public List<Order> getMyOrders() {
        authorizationService.requireAuthenticated();
        Long userId = appSession.getCurrentUser().orElseThrow().userId();
        return orderDAO.findByUserId(userId);
    }

    public PageResult<Order> searchOrdersForAdmin(OrderStatus statusFilter, int page, int pageSize) {
        authorizationService.requireAdmin();
        return orderDAO.findPage(statusFilter, page, pageSize);
    }

    public OrderDetailView getOrderDetail(Long orderId) {
        authorizationService.requireAuthenticated();
        if (orderId == null) {
            throw new ValidationException("Mã đơn hàng không hợp lệ.");
        }
        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));
        UserSession current = appSession.getCurrentUser().orElseThrow();
        if (!current.isAdmin() && !Objects.equals(order.getUserId(), current.userId())) {
            throw new AuthorizationException("Bạn không có quyền truy cập đơn hàng của người dùng khác.");
        }
        List<OrderItem> items = orderDAO.findItemsByOrderId(orderId);
        return new OrderDetailView(order, items);
    }

    public Order updateOrderStatus(Long orderId, OrderStatus nextStatus) {
        authorizationService.requireAdmin();
        if (orderId == null || nextStatus == null) {
            throw new ValidationException("Thông tin cập nhật trạng thái đơn không hợp lệ.");
        }

        try (Connection conn = connectionFactory.getConnection()) {
            boolean prevAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Order locked = orderDAO.findByIdForUpdate(conn, orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));
                if (!locked.getStatus().canTransitionTo(nextStatus)) {
                    throw new ValidationException("status",
                            "Không thể chuyển trạng thái đơn #" + orderId + " từ '"
                                    + locked.getStatus().getDisplayName() + "' sang '"
                                    + nextStatus.getDisplayName() + "'.");
                }

                if (nextStatus == OrderStatus.CANCELLED) {
                    List<OrderItem> items = new ArrayList<>(orderDAO.findItemsByOrderId(orderId));
                    items.sort(Comparator.comparing(OrderItem::getProductId));
                    for (OrderItem item : items) {
                        productDAO.restoreStock(conn, item.getProductId(), item.getQuantity());
                    }
                }

                orderDAO.updateStatus(conn, orderId, nextStatus);
                conn.commit();
                return orderDAO.findById(orderId).orElseThrow();
            } catch (RuntimeException ex) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                throw ex;
            } catch (Exception ex) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                throw new DataAccessException("Lỗi cập nhật trạng thái đơn hàng.", ex);
            } finally {
                try { conn.setAutoCommit(prevAuto); } catch (SQLException ignored) {}
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Không thể kết nối CSDL khi cập nhật đơn hàng.", ex);
        }
    }
}
