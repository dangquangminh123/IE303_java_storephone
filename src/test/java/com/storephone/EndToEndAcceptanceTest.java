package com.storephone;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.storephone.config.AppContext;
import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.controller.NavigationRoutes;
import com.storephone.dto.*;
import com.storephone.model.*;
import com.storephone.security.PasswordHasher;
import com.storephone.util.DatabaseInitializer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EndToEndAcceptanceTest {

    @Test
    void fullJourneyGuestToUserToAdminAndRestartPersistence() throws Exception {
        DatabaseConfig dbConfig = DatabaseConfig.load();
        ConnectionFactory cf = new ConnectionFactory(dbConfig);
        assumeTrue(cf.ping(), "Bỏ qua khi MySQL chưa khởi chạy.");
        DatabaseInitializer.initializeSchema(cf, false);

        AppContext app1 = new AppContext(dbConfig);
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Role adminRole = app1.getRoleDAO().findByName(Role.ADMIN).orElseThrow();
        User admin = new User();
        admin.setRoleId(adminRole.getId());
        admin.setEmail("e2e_admin_" + suffix + "@storephone.vn");
        admin.setPasswordHash(PasswordHasher.hash("AdminPass123"));
        admin.setFullName("Admin E2E");
        admin.setActive(true);
        User savedAdmin = app1.getUserDAO().insert(admin);

        User customer = null;
        Product phone = null;
        Order order = null;

        try {
            app1.getAuthController().login(new LoginRequest(savedAdmin.getEmail(), "AdminPass123".toCharArray()));
            assertTrue(app1.getAppSession().isAdmin());
            phone = app1.getProductService().createProduct(new ProductSaveRequest(
                    "iPhone 17 Pro " + suffix, new BigDecimal("34990000"),
                    "Chip A19 Pro", "Màn hình 120Hz", 4, "Apple", "Chụp ảnh / Quay phim", null, true));
            app1.getAuthController().logout();

            assertFalse(app1.getAppSession().isLoggedIn());
            assertTrue(app1.getNavigationController().navigateTo(NavigationRoutes.CATALOG));
            assertFalse(app1.getNavigationController().navigateTo(NavigationRoutes.CART));
            assertFalse(app1.getNavigationController().navigateTo(NavigationRoutes.ADMIN_DASHBOARD));

            ProductFilterCriteria crit = new ProductFilterCriteria();
            crit.setKeyword("iPhone 17 Pro " + suffix);
            assertEquals(1, app1.getProductService().searchPublicCatalog(crit).getTotalElements());

            customer = app1.getAuthController().register(new RegisterRequest(
                    "Lê Minh Khách", "e2e_u_" + suffix + "@gmail.com", "0988777666", "Đà Nẵng",
                    "UserPass123".toCharArray(), "UserPass123".toCharArray()));
            app1.getAuthController().login(new LoginRequest(customer.getEmail(), "UserPass123".toCharArray()));
            app1.getCartService().addProductToCart(phone.getId(), 2);
            assertEquals(2, app1.getAppSession().getCartQuantity());

            order = app1.getCheckoutService().placeOrder(
                    new CheckoutRequest("Lê Minh Khách", "0988777666", "12 Bạch Đằng, Đà Nẵng"));
            assertEquals(OrderStatus.PENDING, order.getStatus());
            assertEquals(0, app1.getAppSession().getCartQuantity());
            assertEquals(2, app1.getProductDAO().findById(phone.getId()).orElseThrow().getQuantity());

            app1.getAuthController().logout();
            app1.getAuthController().login(new LoginRequest(savedAdmin.getEmail(), "AdminPass123".toCharArray()));
            BigDecimal revBefore = app1.getDashboardService().getDashboardMetrics().completedRevenue();

            app1.getOrderService().updateOrderStatus(order.getId(), OrderStatus.CONFIRMED);
            app1.getOrderService().updateOrderStatus(order.getId(), OrderStatus.SHIPPING);
            app1.getOrderService().updateOrderStatus(order.getId(), OrderStatus.COMPLETED);

            BigDecimal revAfter = app1.getDashboardService().getDashboardMetrics().completedRevenue();
            assertEquals(0, revBefore.add(new BigDecimal("69980000")).compareTo(revAfter));

            // TC-24: App restart
            AppContext app2 = new AppContext(dbConfig);
            assertFalse(app2.getAppSession().isLoggedIn());
            assertEquals(0, app2.getAppSession().getCartQuantity());

            app2.getAuthController().login(new LoginRequest(customer.getEmail(), "UserPass123".toCharArray()));
            Order persisted = app2.getOrderService().getOrderDetail(order.getId()).order();
            assertEquals(OrderStatus.COMPLETED, persisted.getStatus());
            assertEquals(0, new BigDecimal("69980000").compareTo(persisted.getTotalAmount()));
        } finally {
            try (Connection c = cf.getConnection()) {
                if (order != null && order.getId() != null) { exec(c, "DELETE FROM orders WHERE id = ?", order.getId()); }
                if (customer != null && customer.getId() != null) {
                    exec(c, "DELETE FROM carts WHERE user_id = ?", customer.getId());
                    exec(c, "DELETE FROM users WHERE id = ?", customer.getId());
                }
                if (savedAdmin.getId() != null) { exec(c, "DELETE FROM users WHERE id = ?", savedAdmin.getId()); }
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
