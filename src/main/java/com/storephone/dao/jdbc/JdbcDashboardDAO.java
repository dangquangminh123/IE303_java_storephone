package com.storephone.dao.jdbc;

import com.storephone.config.ConnectionFactory;
import com.storephone.dao.DashboardDAO;
import com.storephone.dto.DashboardMetrics;
import com.storephone.exception.DataAccessException;
import com.storephone.model.Order;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcDashboardDAO implements DashboardDAO {
    private final ConnectionFactory cf;

    public JdbcDashboardDAO(ConnectionFactory connectionFactory) {
        this.cf = connectionFactory;
    }

    @Override
    public DashboardMetrics fetchDashboardMetrics() {
        try (Connection c = cf.getConnection()) {
            long activeUsers = queryLong(c, "SELECT COUNT(*) FROM users WHERE active = TRUE");
            long activeProducts = queryLong(c, "SELECT COUNT(*) FROM products WHERE active = TRUE");
            long pendingOrders = queryLong(c, "SELECT COUNT(*) FROM orders WHERE status = 'PENDING'");
            BigDecimal revenue = queryDecimal(c, "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'COMPLETED'");
            List<Order> recentOrders = queryRecentOrders(c);
            List<DashboardMetrics.TopProductStat> topProducts = queryTopProducts(c);
            return new DashboardMetrics(activeUsers, activeProducts, pendingOrders, revenue, recentOrders, topProducts);
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi truy vấn thống kê Dashboard.", ex);
        }
    }

    private static long queryLong(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private static BigDecimal queryDecimal(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            BigDecimal val = rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            return val == null ? BigDecimal.ZERO : val;
        }
    }

    private static List<Order> queryRecentOrders(Connection c) throws SQLException {
        String sql = """
                SELECT id, user_id, receiver_name, receiver_phone, receiver_address, status, total_amount, created_at
                FROM orders ORDER BY created_at DESC, id DESC LIMIT 5
                """;
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(JdbcRowMapper.mapOrder(rs));
            }
        }
        return list;
    }

    private static List<DashboardMetrics.TopProductStat> queryTopProducts(Connection c) throws SQLException {
        String sql = """
                SELECT oi.product_id,
                       MAX(oi.product_name) AS product_name,
                       COALESCE(SUM(oi.quantity), 0) AS sold_qty,
                       COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS sold_revenue
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.status = 'COMPLETED'
                GROUP BY oi.product_id
                ORDER BY sold_qty DESC, sold_revenue DESC
                LIMIT 5
                """;
        List<DashboardMetrics.TopProductStat> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DashboardMetrics.TopProductStat(
                        rs.getLong("product_id"),
                        rs.getString("product_name"),
                        rs.getLong("sold_qty"),
                        rs.getBigDecimal("sold_revenue")
                ));
            }
        }
        return list;
    }
}
