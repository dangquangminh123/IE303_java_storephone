package com.storephone.dao.jdbc;

import com.storephone.model.Cart;
import com.storephone.model.CartItem;
import com.storephone.model.Order;
import com.storephone.model.OrderItem;
import com.storephone.model.OrderStatus;
import com.storephone.model.Product;
import com.storephone.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

final class JdbcRowMapper {
    private JdbcRowMapper() {
    }

    static User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setRoleId(rs.getLong("role_id"));
        u.setRoleName(rs.getString("role_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        u.setAddress(rs.getString("address"));
        u.setAvatarPath(rs.getString("avatar_path"));
        u.setActive(rs.getBoolean("active"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            u.setCreatedAt(created.toLocalDateTime());
        }
        return u;
    }

    static Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setShortDesc(rs.getString("short_desc"));
        p.setDetailDesc(rs.getString("detail_desc"));
        p.setQuantity(rs.getInt("quantity"));
        p.setFactory(rs.getString("factory"));
        p.setTarget(rs.getString("target"));
        p.setImagePath(rs.getString("image_path"));
        p.setActive(rs.getBoolean("active"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            p.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            p.setUpdatedAt(updated.toLocalDateTime());
        }
        return p;
    }

    static Cart mapCart(ResultSet rs) throws SQLException {
        Cart c = new Cart();
        c.setId(rs.getLong("id"));
        c.setUserId(rs.getLong("user_id"));
        c.setTotalQuantity(rs.getInt("total_quantity"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            c.setCreatedAt(created.toLocalDateTime());
        }
        return c;
    }

    static CartItem mapCartItem(ResultSet rs) throws SQLException {
        CartItem item = new CartItem();
        item.setId(rs.getLong("id"));
        item.setCartId(rs.getLong("cart_id"));
        item.setProductId(rs.getLong("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        return item;
    }

    static Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setUserId(rs.getLong("user_id"));
        o.setReceiverName(rs.getString("receiver_name"));
        o.setReceiverPhone(rs.getString("receiver_phone"));
        o.setReceiverAddress(rs.getString("receiver_address"));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            o.setCreatedAt(created.toLocalDateTime());
        }
        return o;
    }

    static OrderItem mapOrderItem(ResultSet rs) throws SQLException {
        OrderItem item = new OrderItem();
        item.setId(rs.getLong("id"));
        item.setOrderId(rs.getLong("order_id"));
        item.setProductId(rs.getLong("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        return item;
    }
}
