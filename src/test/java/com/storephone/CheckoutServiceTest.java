package com.storephone;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.dao.*;
import com.storephone.dao.jdbc.*;
import com.storephone.dto.CheckoutRequest;
import com.storephone.dto.LoginRequest;
import com.storephone.dto.RegisterRequest;
import com.storephone.exception.DataAccessException;
import com.storephone.exception.InsufficientStockException;
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

class CheckoutServiceTest {

    @Test
    void shouldValidateRollbackOnFaultsAndCommitAtomically() throws Exception {
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

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = null;
        Product prod = null;
        Order placed = null;

        try {
            user = authService.register(new RegisterRequest(
                    "Khách Đặt", "chk_" + suffix + "@gmail.com", "0912345678", "100 Nguyễn Huệ",
                    "MatKhau1234".toCharArray(), "MatKhau1234".toCharArray()));
            authService.login(new LoginRequest(user.getEmail(), "MatKhau1234".toCharArray()));
            CheckoutRequest validReq = new CheckoutRequest("Khách Đặt", "0912345678", "100 Nguyễn Huệ");

            // TC-14: Giỏ rỗng
            assertThrows(ValidationException.class, () -> checkoutService.placeOrder(validReq));

            Product p = new Product();
            p.setName("CheckoutPhone " + suffix);
            p.setPrice(new BigDecimal("15000000"));
            p.setQuantity(3);
            p.setFactory("Apple");
            p.setTarget("Mỏng nhẹ / Thời trang");
            p.setShortDesc("Test");
            p.setActive(true);
            prod = productDAO.insert(p);
            cartService.addProductToCart(prod.getId(), 2);

            // TC-15: Thiếu SĐT
            assertThrows(ValidationException.class, () -> checkoutService.placeOrder(
                    new CheckoutRequest("Khách", "", "100 Nguyễn Huệ")));
            assertEquals(0, orderDAO.findByUserId(user.getId()).size());

            // TC-17: Kho không đủ (qty=2, kho=1) -> rollback
            prod.setQuantity(1);
            productDAO.update(prod);
            assertThrows(InsufficientStockException.class, () -> checkoutService.placeOrder(validReq));
            assertEquals(1, productDAO.findById(prod.getId()).orElseThrow().getQuantity());
            assertEquals(2, cartService.getMyCart().totalQuantity());

            // TC-18: Lỗi SQL sau insertOrder -> rollback, không có order mồ côi
            prod.setQuantity(3);
            productDAO.update(prod);
            assertThrows(DataAccessException.class, () -> checkoutService.placeOrderWithFaultHook(
                    validReq, () -> { throw new DataAccessException("Simulated SQL error", new java.sql.SQLException("Mock")); }));
            assertEquals(3, productDAO.findById(prod.getId()).orElseThrow().getQuantity());
            assertEquals(2, cartService.getMyCart().totalQuantity());
            assertEquals(0, orderDAO.findByUserId(user.getId()).size());

            // TC-16: Đặt hàng thành công (qty=2, kho=3) -> kho còn 1, giỏ về 0
            placed = checkoutService.placeOrder(validReq);
            assertEquals(OrderStatus.PENDING, placed.getStatus());
            assertEquals(1, productDAO.findById(prod.getId()).orElseThrow().getQuantity());
            assertEquals(0, cartService.getMyCart().totalQuantity());
            assertEquals(0, session.getCartQuantity());
        } finally {
            try (Connection c = cf.getConnection()) {
                if (placed != null && placed.getId() != null) { exec(c, "DELETE FROM orders WHERE id = ?", placed.getId()); }
                if (user != null && user.getId() != null) {
                    exec(c, "DELETE FROM carts WHERE user_id = ?", user.getId());
                    exec(c, "DELETE FROM users WHERE id = ?", user.getId());
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
