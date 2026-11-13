package com.storephone;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.dao.CartDAO;
import com.storephone.dao.ProductDAO;
import com.storephone.dao.RoleDAO;
import com.storephone.dao.UserDAO;
import com.storephone.dao.jdbc.JdbcCartDAO;
import com.storephone.dao.jdbc.JdbcProductDAO;
import com.storephone.dao.jdbc.JdbcRoleDAO;
import com.storephone.dao.jdbc.JdbcUserDAO;
import com.storephone.dto.CartSummary;
import com.storephone.dto.LoginRequest;
import com.storephone.dto.RegisterRequest;
import com.storephone.exception.InsufficientStockException;
import com.storephone.exception.ValidationException;
import com.storephone.model.Product;
import com.storephone.model.User;
import com.storephone.security.AppSession;
import com.storephone.service.AuthService;
import com.storephone.service.AuthorizationService;
import com.storephone.service.CartService;
import com.storephone.util.DatabaseInitializer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartServiceTest {

    @Test
    void shouldAccumulateDuplicateProductSyncQuantityEnforceStockAndPersistAcrossRelogin() throws Exception {
        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        assumeTrue(cf.ping(), "Bỏ qua khi MySQL chưa khởi chạy.");
        DatabaseInitializer.initializeSchema(cf, false);

        RoleDAO roleDAO = new JdbcRoleDAO(cf);
        UserDAO userDAO = new JdbcUserDAO(cf);
        ProductDAO productDAO = new JdbcProductDAO(cf);
        CartDAO cartDAO = new JdbcCartDAO(cf);
        AppSession session = new AppSession();
        AuthService authService = new AuthService(userDAO, roleDAO, cartDAO, session);
        CartService cartService = new CartService(cartDAO, productDAO, session, new AuthorizationService(session));

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = null;
        Product prodA = null;
        Product prodB = null;

        try {
            user = authService.register(new RegisterRequest(
                    "Khách Giỏ Hàng", "cart_" + suffix + "@gmail.com", "0912345678", "TP.HCM",
                    "MatKhau1234".toCharArray(), "MatKhau1234".toCharArray()));
            authService.login(new LoginRequest(user.getEmail(), "MatKhau1234".toCharArray()));

            Product a = new Product();
            a.setName("Phone A " + suffix);
            a.setPrice(new BigDecimal("10000000"));
            a.setQuantity(3);
            a.setFactory("Apple");
            a.setTarget("Mỏng nhẹ / Thời trang");
            a.setShortDesc("A");
            a.setActive(true);
            prodA = productDAO.insert(a);

            Product b = new Product();
            b.setName("Phone B " + suffix);
            b.setPrice(new BigDecimal("5000000"));
            b.setQuantity(5);
            b.setFactory("Xiaomi");
            b.setTarget("Pin trâu / Phổ thông");
            b.setShortDesc("B");
            b.setActive(true);
            prodB = productDAO.insert(b);

            // TC-11: Thêm A 2 lần -> chỉ 1 dòng cart_item với quantity = 2
            cartService.addProductToCart(prodA.getId(), 1);
            CartSummary afterA = cartService.addProductToCart(prodA.getId(), 1);
            assertEquals(1, afterA.items().size());
            assertEquals(2, afterA.items().get(0).getQuantity());
            assertEquals(2, session.getCartQuantity());

            // P08-05: Vượt tồn kho (2 + 2 > 3) hoặc qty <= 0 bị chặn
            final Long idA = prodA.getId();
            assertThrows(InsufficientStockException.class, () -> cartService.addProductToCart(idA, 2));
            assertThrows(ValidationException.class, () -> cartService.addProductToCart(idA, 0));

            // TC-12: Thêm B qty=4 rồi xóa B -> tổng số lượng giảm đúng 4
            CartSummary withB = cartService.addProductToCart(prodB.getId(), 4);
            assertEquals(6, withB.totalQuantity());
            assertEquals(new BigDecimal("40000000"), withB.totalAmount().stripTrailingZeros().setScale(0));

            final Long idB = prodB.getId();
            Long itemBId = withB.items().stream()
                    .filter(i -> i.getProductId().equals(idB)).findFirst().orElseThrow().getId();
            CartSummary afterRemoveB = cartService.removeItem(itemBId);
            assertEquals(2, afterRemoveB.totalQuantity());
            assertEquals(2, session.getCartQuantity());

            // TC-13: Đăng xuất rồi đăng nhập lại -> giỏ hàng được khôi phục nguyên vẹn từ MySQL
            authService.logout();
            assertEquals(0, session.getCartQuantity());

            authService.login(new LoginRequest(user.getEmail(), "MatKhau1234".toCharArray()));
            assertEquals(2, session.getCartQuantity());
            assertEquals(2, cartService.getMyCart().totalQuantity());
        } finally {
            try (Connection c = cf.getConnection()) {
                if (user != null && user.getId() != null) {
                    exec(c, "DELETE FROM carts WHERE user_id = ?", user.getId());
                    exec(c, "DELETE FROM users WHERE id = ?", user.getId());
                }
                if (prodA != null && prodA.getId() != null) { exec(c, "DELETE FROM products WHERE id = ?", prodA.getId()); }
                if (prodB != null && prodB.getId() != null) { exec(c, "DELETE FROM products WHERE id = ?", prodB.getId()); }
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
