package com.storephone;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.dao.*;
import com.storephone.dao.jdbc.*;
import com.storephone.dto.*;
import com.storephone.exception.AuthorizationException;
import com.storephone.exception.ValidationException;
import com.storephone.model.*;
import com.storephone.security.AppSession;
import com.storephone.service.*;
import com.storephone.util.DatabaseInitializer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    @Test
    void shouldEnforceOwnershipSnapshotPriceAndSingleCancelRestore() throws Exception {
        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        assumeTrue(cf.ping(), "Bỏ qua khi MySQL chưa khởi chạy.");
        DatabaseInitializer.initializeSchema(cf, false);

        RoleDAO roleDAO = new JdbcRoleDAO(cf);
        UserDAO userDAO = new JdbcUserDAO(cf);
        ProductDAO productDAO = new JdbcProductDAO(cf);
        CartDAO cartDAO = new JdbcCartDAO(cf);
        OrderDAO orderDAO = new JdbcOrderDAO(cf);

        AppSession session = new AppSession();
        AuthorizationService authz = new AuthorizationService(session);
        AuthService authService = new AuthService(userDAO, roleDAO, cartDAO, session);
        CartService cartService = new CartService(cartDAO, productDAO, session, authz);
        CheckoutService checkoutService = new CheckoutService(cf, cartDAO, productDAO, orderDAO, session, authz);
        OrderService orderService = new OrderService(cf, orderDAO, productDAO, session, authz);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User userA = null;
        User userB = null;
        Product prod = null;
        Order orderA = null;

        try {
            userA = authService.register(new RegisterRequest(
                    "User A", "ua_" + suffix + "@gmail.com", "0911111111", "Q1",
                    "MatKhau1234".toCharArray(), "MatKhau1234".toCharArray()));
            userB = authService.register(new RegisterRequest(
                    "User B", "ub_" + suffix + "@gmail.com", "0922222222", "Q2",
                    "MatKhau1234".toCharArray(), "MatKhau1234".toCharArray()));

            Product p = new Product();
            p.setName("OrderPhone " + suffix);
            p.setPrice(new BigDecimal("20000000"));
            p.setQuantity(5);
            p.setFactory("Samsung");
            p.setTarget("Chụp ảnh / Quay phim");
            p.setShortDesc("Test");
            p.setActive(true);
            prod = productDAO.insert(p);

            authService.login(new LoginRequest(userA.getEmail(), "MatKhau1234".toCharArray()));
            cartService.addProductToCart(prod.getId(), 2);
            orderA = checkoutService.placeOrder(new CheckoutRequest("User A", "0911111111", "Q1, TP.HCM"));
            assertEquals(3, productDAO.findById(prod.getId()).orElseThrow().getQuantity());

            // TC-19: User B bị từ chối xem đơn của User A
            final Long orderId = orderA.getId();
            authService.login(new LoginRequest(userB.getEmail(), "MatKhau1234".toCharArray()));
            assertThrows(AuthorizationException.class, () -> orderService.getOrderDetail(orderId));
            assertTrue(orderService.getMyOrders().isEmpty());

            // TC-22: Sửa giá Product sau khi đặt -> Order detail giữ giá snapshot cũ
            prod = productDAO.findById(prod.getId()).orElseThrow();
            prod.setPrice(new BigDecimal("29000000"));
            productDAO.update(prod);
            authService.login(new LoginRequest(userA.getEmail(), "MatKhau1234".toCharArray()));
            OrderDetailView detail = orderService.getOrderDetail(orderId);
            assertEquals(new BigDecimal("20000000"), detail.items().get(0).getUnitPrice().stripTrailingZeros().setScale(0));

            session.login(new UserSession(1L, "admin@test.vn", "Admin", Role.ADMIN));
            orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);

            // TC-20: ADMIN hủy đơn chưa giao -> hoàn kho 3 + 2 = 5
            Order cancelled = orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
            assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
            assertEquals(5, productDAO.findById(prod.getId()).orElseThrow().getQuantity());

            // TC-21: Hủy lại đơn đã CANCELLED -> không hoàn kho lần 2
            assertThrows(ValidationException.class, () -> orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED));
            assertEquals(5, productDAO.findById(prod.getId()).orElseThrow().getQuantity());
        } finally {
            try (Connection c = cf.getConnection()) {
                if (orderA != null && orderA.getId() != null) { exec(c, "DELETE FROM orders WHERE id = ?", orderA.getId()); }
                if (userA != null && userA.getId() != null) {
                    exec(c, "DELETE FROM carts WHERE user_id = ?", userA.getId());
                    exec(c, "DELETE FROM users WHERE id = ?", userA.getId());
                }
                if (userB != null && userB.getId() != null) {
                    exec(c, "DELETE FROM carts WHERE user_id = ?", userB.getId());
                    exec(c, "DELETE FROM users WHERE id = ?", userB.getId());
                }
                if (prod != null && prod.getId() != null) { exec(c, "DELETE FROM products WHERE id = ?", prod.getId()); }
            }
        }
    }

    private static void exec(Connection c, String sql, Long id) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
