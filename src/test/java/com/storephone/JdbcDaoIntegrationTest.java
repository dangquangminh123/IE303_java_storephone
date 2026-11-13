package com.storephone;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.dao.*;
import com.storephone.dao.jdbc.*;
import com.storephone.dto.PageResult;
import com.storephone.dto.ProductFilterCriteria;
import com.storephone.model.*;
import com.storephone.security.PasswordHasher;
import com.storephone.util.DatabaseInitializer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JdbcDaoIntegrationTest {

    @Test
    void jdbcDaosShouldExecuteCrudAndAtomicCheckoutAgainstMysql() throws Exception {
        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        assumeTrue(cf.ping(), "Bỏ qua kiểm thử tích hợp khi MySQL chưa bật.");

        DatabaseInitializer.initializeSchema(cf, false);

        RoleDAO roleDAO = new JdbcRoleDAO(cf);
        UserDAO userDAO = new JdbcUserDAO(cf);
        ProductDAO productDAO = new JdbcProductDAO(cf);
        CartDAO cartDAO = new JdbcCartDAO(cf);
        OrderDAO orderDAO = new JdbcOrderDAO(cf);

        Role userRole = roleDAO.findByName(Role.USER).orElseThrow();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User savedUser = null;
        Product savedProduct = null;
        Order savedOrder = null;

        try {
            User u = new User();
            u.setRoleId(userRole.getId());
            u.setEmail("it_" + suffix + "@storephone.vn");
            u.setPasswordHash(PasswordHasher.hash("MatKhau123!"));
            u.setFullName("Người Dùng Kiểm Thử");
            u.setPhone("0987654321");
            u.setAddress("123 Nguyễn Huệ, TP.HCM");
            u.setActive(true);
            savedUser = userDAO.insert(u);
            assertEquals(Role.USER, savedUser.getRoleName());

            Product p = new Product();
            p.setName("TestPhone " + suffix);
            p.setPrice(new BigDecimal("19990000.00"));
            p.setShortDesc("Điện thoại kiểm thử");
            p.setDetailDesc("Mô tả chi tiết");
            p.setQuantity(5);
            p.setFactory("TestBrand");
            p.setTarget("Chơi game / Cấu hình cao");
            p.setActive(true);
            savedProduct = productDAO.insert(p);

            ProductFilterCriteria criteria = new ProductFilterCriteria();
            criteria.setKeyword("TestPhone");
            criteria.setFactories(List.of("TestBrand"));
            criteria.setSortBy(ProductFilterCriteria.SortBy.PRICE_ASC);
            PageResult<Product> filtered = productDAO.search(criteria);
            assertTrue(filtered.getTotalElements() >= 1);

            Cart cart = cartDAO.getOrCreateByUserId(savedUser.getId());
            cartDAO.upsertItem(cart.getId(), savedProduct.getId(), 2, savedProduct.getPrice());
            assertEquals(2, cartDAO.getOrCreateByUserId(savedUser.getId()).getTotalQuantity());

            try (Connection conn = cf.getConnection()) {
                conn.setAutoCommit(false);
                assertTrue(productDAO.deductStock(conn, savedProduct.getId(), 2));

                Order order = new Order();
                order.setUserId(savedUser.getId());
                order.setReceiverName(savedUser.getFullName());
                order.setReceiverAddress(savedUser.getAddress());
                order.setReceiverPhone(savedUser.getPhone());
                order.setTotalAmount(new BigDecimal("39980000.00"));
                order.setStatus(OrderStatus.PENDING);

                savedOrder = orderDAO.insertOrder(conn, order);
                OrderItem oi = new OrderItem();
                oi.setProductId(savedProduct.getId());
                oi.setProductName(savedProduct.getName());
                oi.setQuantity(2);
                oi.setUnitPrice(savedProduct.getPrice());
                orderDAO.insertOrderItems(conn, savedOrder.getId(), List.of(oi));
                cartDAO.clearCart(conn, cart.getId());
                conn.commit();
            }

            assertEquals(3, productDAO.findById(savedProduct.getId()).orElseThrow().getQuantity());
            assertEquals(0, cartDAO.getOrCreateByUserId(savedUser.getId()).getTotalQuantity());
            assertEquals(1, orderDAO.findItemsByOrderId(savedOrder.getId()).size());
        } finally {
            cleanup(cf, savedOrder, savedUser, savedProduct);
        }
    }

    private static void cleanup(ConnectionFactory cf, Order o, User u, Product p) {
        try (Connection c = cf.getConnection()) {
            if (o != null && o.getId() != null) {
                execDelete(c, "DELETE FROM orders WHERE id=?", o.getId());
            }
            if (u != null && u.getId() != null) {
                execDelete(c, "DELETE FROM carts WHERE user_id=?", u.getId());
                execDelete(c, "DELETE FROM users WHERE id=?", u.getId());
            }
            if (p != null && p.getId() != null) {
                execDelete(c, "DELETE FROM products WHERE id=?", p.getId());
            }
        } catch (Exception ignored) {
        }
    }

    private static void execDelete(Connection c, String sql, Long id) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
