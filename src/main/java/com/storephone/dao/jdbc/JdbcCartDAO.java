package com.storephone.dao.jdbc;

import com.storephone.config.ConnectionFactory;
import com.storephone.dao.CartDAO;
import com.storephone.exception.DataAccessException;
import com.storephone.model.Cart;
import com.storephone.model.CartItem;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCartDAO implements CartDAO {
    private final ConnectionFactory cf;

    public JdbcCartDAO(ConnectionFactory connectionFactory) {
        this.cf = connectionFactory;
    }

    @Override
    public Cart getOrCreateByUserId(Long userId) {
        Optional<Cart> existing = findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        String sql = "INSERT INTO carts (user_id, total_quantity) VALUES (?, 0)";
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    Cart cart = new Cart();
                    cart.setId(keys.getLong(1));
                    cart.setUserId(userId);
                    cart.setTotalQuantity(0);
                    return cart;
                }
            }
            return findByUserId(userId).orElseThrow();
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                return findByUserId(userId).orElseThrow();
            }
            throw new DataAccessException("Lỗi khởi tạo giỏ hàng.", ex);
        }
    }

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, user_id, total_quantity, created_at FROM carts WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(JdbcRowMapper.mapCart(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi truy vấn giỏ hàng.", ex);
        }
    }

    @Override
    public List<CartItem> findItemsByCartId(Long cartId) {
        String sql = """
                SELECT ci.id, ci.cart_id, ci.product_id, p.name AS product_name, ci.quantity, ci.unit_price
                FROM cart_items ci JOIN products p ON p.id = ci.product_id
                WHERE ci.cart_id = ? ORDER BY ci.id ASC
                """;
        List<CartItem> items = new ArrayList<>();
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(JdbcRowMapper.mapCartItem(rs));
                }
            }
            return items;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi lấy danh sách sản phẩm trong giỏ.", ex);
        }
    }

    @Override
    public Optional<CartItem> findItemByCartAndProduct(Long cartId, Long productId) {
        String sql = """
                SELECT ci.id, ci.cart_id, ci.product_id, p.name AS product_name, ci.quantity, ci.unit_price
                FROM cart_items ci JOIN products p ON p.id = ci.product_id
                WHERE ci.cart_id = ? AND ci.product_id = ?
                """;
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cartId);
            ps.setLong(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(JdbcRowMapper.mapCartItem(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi kiểm tra dòng giỏ hàng.", ex);
        }
    }

    @Override
    public void upsertItem(Long cartId, Long productId, int quantity, BigDecimal unitPrice) {
        String sql = """
                INSERT INTO cart_items (cart_id, product_id, quantity, unit_price)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), unit_price = VALUES(unit_price)
                """;
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cartId);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, unitPrice);
            ps.executeUpdate();
            syncTotalQuantity(c, cartId);
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi thêm/cập nhật sản phẩm trong giỏ.", ex);
        }
    }

    @Override
    public void updateItemQuantity(Long cartItemId, int quantity) {
        try (Connection c = cf.getConnection()) {
            Long cartId = findCartIdByItemId(c, cartItemId);
            try (PreparedStatement ps = c.prepareStatement("UPDATE cart_items SET quantity = ? WHERE id = ?")) {
                ps.setInt(1, quantity);
                ps.setLong(2, cartItemId);
                ps.executeUpdate();
            }
            if (cartId != null) {
                syncTotalQuantity(c, cartId);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi cập nhật số lượng giỏ hàng.", ex);
        }
    }

    @Override
    public void deleteItem(Long cartItemId) {
        try (Connection c = cf.getConnection()) {
            Long cartId = findCartIdByItemId(c, cartItemId);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM cart_items WHERE id = ?")) {
                ps.setLong(1, cartItemId);
                ps.executeUpdate();
            }
            if (cartId != null) {
                syncTotalQuantity(c, cartId);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi xóa sản phẩm khỏi giỏ hàng.", ex);
        }
    }

    @Override
    public void syncTotalQuantity(Long cartId) {
        try (Connection c = cf.getConnection()) {
            syncTotalQuantity(c, cartId);
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi đồng bộ số lượng giỏ hàng.", ex);
        }
    }

    @Override
    public void syncTotalQuantity(Connection conn, Long cartId) {
        String sql = "UPDATE carts SET total_quantity = COALESCE((SELECT SUM(quantity) FROM cart_items WHERE cart_id = ?), 0) WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cartId);
            ps.setLong(2, cartId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi đồng bộ tổng số lượng giỏ hàng.", ex);
        }
    }

    @Override
    public void clearCart(Connection conn, Long cartId) {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM cart_items WHERE cart_id = ?");
             PreparedStatement upd = conn.prepareStatement("UPDATE carts SET total_quantity = 0 WHERE id = ?")) {
            del.setLong(1, cartId);
            del.executeUpdate();
            upd.setLong(1, cartId);
            upd.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi làm rỗng giỏ hàng sau đặt hàng.", ex);
        }
    }

    private static Long findCartIdByItemId(Connection c, Long cartItemId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT cart_id FROM cart_items WHERE id = ?")) {
            ps.setLong(1, cartItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }
}
