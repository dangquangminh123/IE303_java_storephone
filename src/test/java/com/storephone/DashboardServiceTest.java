package com.storephone;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.dao.*;
import com.storephone.dao.jdbc.*;
import com.storephone.dto.*;
import com.storephone.exception.AuthorizationException;
import com.storephone.model.*;
import com.storephone.security.AppSession;
import com.storephone.service.*;
import com.storephone.util.DatabaseInitializer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    @Test
    void shouldRestrictToAdminAndReflectCompletedOrderRevenueAndPendingCounts() throws Exception {
        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        assumeTrue(cf.ping(), "Bỏ qua khi MySQL chưa khởi chạy.");
        DatabaseInitializer.initializeSchema(cf, false);

        RoleDAO roleDAO = new JdbcRoleDAO(cf);
        UserDAO userDAO = new JdbcUserDAO(cf);
        ProductDAO productDAO = new JdbcProductDAO(cf);
        CartDAO cartDAO = new JdbcCartDAO(cf);
        OrderDAO orderDAO = new JdbcOrderDAO(cf);
        DashboardDAO dashboardDAO = new JdbcDashboardDAO(cf);

        AppSession session = new AppSession();
        AuthorizationService authz = new AuthorizationService(session);
        AuthService authService = new AuthService(userDAO, roleDAO, cartDAO, session);
        CartService cartService = new CartService(cartDAO, productDAO, session, authz);
        CheckoutService checkoutService = new CheckoutService(cf, cartDAO, productDAO, orderDAO, session, authz);
        OrderService orderService = new OrderService(cf, orderDAO, productDAO, session, authz);
        DashboardService dashboardService = new DashboardService(dashboardDAO, authz);

        // P11-05: USER bị từ chối xem Dashboard
        session.login(new UserSession(99L, "u@test.vn", "User", Role.USER));
        assertThrows(AuthorizationException.class, dashboardService::getDashboardMetrics);

        session.login(new UserSession(1L, "admin@test.vn", "Admin", Role.ADMIN));
        DashboardMetrics baseline = dashboardService.getDashboardMetrics();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User buyer = null;
        Product phone = null;
        Order order = null;

        try {
            buyer = authService.register(new RegisterRequest(
                    "Buyer Dash", "dash_" + suffix + "@gmail.com", "0912345678", "TP.HCM",
                    "MatKhau1234".toCharArray(), "MatKhau1234".toCharArray()));
            Product p = new Product();
            p.setName("DashPhone " + suffix);
            p.setPrice(new BigDecimal("12500000"));
            p.setQuantity(10);
            p.setFactory("OPPO");
            p.setTarget("Mỏng nhẹ / Thời trang");
            p.setShortDesc("Test");
            p.setActive(true);
            phone = productDAO.insert(p);

            authService.login(new LoginRequest(buyer.getEmail(), "MatKhau1234".toCharArray()));
            cartService.addProductToCart(phone.getId(), 2); // 25.000.000 ₫
            order = checkoutService.placeOrder(new CheckoutRequest("Buyer Dash", "0912345678", "TP.HCM"));

            // Khi đơn còn PENDING: pendingOrders tăng 1, completedRevenue chưa đổi
            session.login(new UserSession(1L, "admin@test.vn", "Admin", Role.ADMIN));
            DashboardMetrics afterPending = dashboardService.getDashboardMetrics();
            assertEquals(baseline.pendingOrderCount() + 1, afterPending.pendingOrderCount());
            assertEquals(0, baseline.completedRevenue().compareTo(afterPending.completedRevenue()));

            // TC-23: Chuyển đơn PENDING -> CONFIRMED -> SHIPPING -> COMPLETED -> revenue tăng đúng 25.000.000
            orderService.updateOrderStatus(order.getId(), OrderStatus.CONFIRMED);
            orderService.updateOrderStatus(order.getId(), OrderStatus.SHIPPING);
            orderService.updateOrderStatus(order.getId(), OrderStatus.COMPLETED);

            DashboardMetrics afterCompleted = dashboardService.getDashboardMetrics();
            BigDecimal expectedRev = baseline.completedRevenue().add(new BigDecimal("25000000"));
            assertEquals(0, expectedRev.compareTo(afterCompleted.completedRevenue()));
        } finally {
            try (Connection c = cf.getConnection()) {
                if (order != null && order.getId() != null) { exec(c, "DELETE FROM orders WHERE id = ?", order.getId()); }
                if (buyer != null && buyer.getId() != null) {
                    exec(c, "DELETE FROM carts WHERE user_id = ?", buyer.getId());
                    exec(c, "DELETE FROM users WHERE id = ?", buyer.getId());
                }
                if (phone != null && phone.getId() != null) { exec(c, "DELETE FROM products WHERE id = ?", phone.getId()); }
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
