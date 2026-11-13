package com.storephone.dao.jdbc;

import com.storephone.config.ConnectionFactory;
import com.storephone.dao.OrderDAO;
import com.storephone.dto.PageResult;
import com.storephone.exception.DataAccessException;
import com.storephone.model.Order;
import com.storephone.model.OrderItem;
import com.storephone.model.OrderStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcOrderDAO implements OrderDAO {
    private static final String BASE_SELECT = """
            SELECT id, user_id, receiver_name, receiver_phone, receiver_address,
                   status, total_amount, created_at
            FROM orders
            """;

    private final ConnectionFactory cf;

    public JdbcOrderDAO(ConnectionFactory connectionFactory) {
        this.cf = connectionFactory;
    }

    @Override
    public Order insertOrder(Connection conn, Order order) {
        String sql = "INSERT INTO orders (user_id, receiver_name, receiver_phone, receiver_address, status, total_amount) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, order.getUserId());
            ps.setString(2, order.getReceiverName());
            ps.setString(3, order.getReceiverPhone());
            ps.setString(4, order.getReceiverAddress());
            ps.setString(5, order.getStatus().name());
            ps.setBigDecimal(6, order.getTotalAmount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setId(keys.getLong(1));
                }
            }
            return order;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi tạo đơn hàng.", ex);
        }
    }

    @Override
    public void insertOrderItems(Connection conn, Long orderId, List<OrderItem> items) {
        String sql = "INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (OrderItem item : items) {
                ps.setLong(1, orderId);
                ps.setLong(2, item.getProductId());
                ps.setString(3, item.getProductName());
                ps.setInt(4, item.getQuantity());
                ps.setBigDecimal(5, item.getUnitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi lưu chi tiết đơn hàng.", ex);
        }
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + " WHERE id = ?")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(JdbcRowMapper.mapOrder(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi truy vấn đơn hàng theo ID.", ex);
        }
    }

    @Override
    public Optional<Order> findByIdForUpdate(Connection conn, Long orderId) {
        try (PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(JdbcRowMapper.mapOrder(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi khóa đơn hàng khi cập nhật trạng thái.", ex);
        }
    }

    @Override
    public List<OrderItem> findItemsByOrderId(Long orderId) {
        String sql = "SELECT id, order_id, product_id, product_name, quantity, unit_price FROM order_items WHERE order_id = ? ORDER BY id ASC";
        List<OrderItem> items = new ArrayList<>();
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(JdbcRowMapper.mapOrderItem(rs));
                }
            }
            return items;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi lấy chi tiết đơn hàng.", ex);
        }
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        List<Order> list = new ArrayList<>();
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + " WHERE user_id = ? ORDER BY created_at DESC, id DESC")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(JdbcRowMapper.mapOrder(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi lấy lịch sử đơn hàng.", ex);
        }
    }

    @Override
    public PageResult<Order> findPage(OrderStatus statusFilter, int page, int pageSize) {
        int p = Math.max(1, page);
        int sz = Math.max(1, pageSize);
        String where = statusFilter != null ? " WHERE status = ?" : "";

        try (Connection c = cf.getConnection()) {
            long total;
            try (PreparedStatement cps = c.prepareStatement("SELECT COUNT(*) FROM orders" + where)) {
                if (statusFilter != null) {
                    cps.setString(1, statusFilter.name());
                }
                try (ResultSet rs = cps.executeQuery()) {
                    total = rs.next() ? rs.getLong(1) : 0L;
                }
            }
            List<Order> list = new ArrayList<>();
            try (PreparedStatement dps = c.prepareStatement(BASE_SELECT + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?")) {
                int i = 1;
                if (statusFilter != null) {
                    dps.setString(i++, statusFilter.name());
                }
                dps.setInt(i++, sz);
                dps.setInt(i, (p - 1) * sz);
                try (ResultSet rs = dps.executeQuery()) {
                    while (rs.next()) {
                        list.add(JdbcRowMapper.mapOrder(rs));
                    }
                }
            }
            return new PageResult<>(list, total, p, sz);
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi phân trang đơn hàng.", ex);
        }
    }

    @Override
    public void updateStatus(Connection conn, Long orderId, OrderStatus newStatus) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status = ? WHERE id = ?")) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, orderId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi cập nhật trạng thái đơn hàng.", ex);
        }
    }

    @Override
    public long countAll() {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM orders");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi đếm tổng đơn hàng.", ex);
        }
    }

    @Override
    public BigDecimal sumCompletedRevenue() {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'COMPLETED'";
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi thống kê doanh thu đơn hoàn tất.", ex);
        }
    }
}
