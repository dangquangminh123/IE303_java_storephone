package com.storephone;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.dao.CartDAO;
import com.storephone.dao.RoleDAO;
import com.storephone.dao.UserDAO;
import com.storephone.dao.jdbc.JdbcCartDAO;
import com.storephone.dao.jdbc.JdbcRoleDAO;
import com.storephone.dao.jdbc.JdbcUserDAO;
import com.storephone.dto.LoginRequest;
import com.storephone.dto.RegisterRequest;
import com.storephone.dto.UserSession;
import com.storephone.exception.DuplicateEmailException;
import com.storephone.exception.ValidationException;
import com.storephone.model.Cart;
import com.storephone.model.Role;
import com.storephone.model.User;
import com.storephone.security.AppSession;
import com.storephone.security.PasswordHasher;
import com.storephone.service.AuthService;
import com.storephone.util.DatabaseInitializer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    @Test
    void shouldRegisterLoginEnforceLockedAccountAndLogoutCleanly() throws Exception {
        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        assumeTrue(cf.ping(), "Bỏ qua khi MySQL chưa khởi chạy.");
        DatabaseInitializer.initializeSchema(cf, false);

        RoleDAO roleDAO = new JdbcRoleDAO(cf);
        UserDAO userDAO = new JdbcUserDAO(cf);
        CartDAO cartDAO = new JdbcCartDAO(cf);
        AppSession session = new AppSession();
        AuthService authService = new AuthService(userDAO, roleDAO, cartDAO, session);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "  TC01_" + suffix + "@Gmail.com ";
        User createdUser = null;

        try {
            // TC-01: Register user hợp lệ -> gán cố định role USER + băm BCrypt
            RegisterRequest regReq = new RegisterRequest(
                    "Nguyễn Văn An",
                    email,
                    "0912345678",
                    "12 Lê Lợi, Q.1",
                    "MatKhauManh123".toCharArray(),
                    "MatKhauManh123".toCharArray()
            );
            createdUser = authService.register(regReq);
            assertNotNull(createdUser.getId());
            assertEquals("tc01_" + suffix + "@gmail.com", createdUser.getEmail());
            assertEquals(Role.USER, createdUser.getRoleName());
            assertTrue(createdUser.getPasswordHash().startsWith("$2a$12$"));
            assertTrue(PasswordHasher.verify("MatKhauManh123", createdUser.getPasswordHash()));

            // TC-02: Đăng ký trùng email -> ném DuplicateEmailException, không thêm bản ghi mới
            final String dupEmail = createdUser.getEmail();
            assertThrows(DuplicateEmailException.class, () -> authService.register(new RegisterRequest(
                    "Người Khác",
                    dupEmail.toUpperCase(),
                    null,
                    null,
                    "MatKhauManh123".toCharArray(),
                    "MatKhauManh123".toCharArray()
            )));

            // TC-03: Đăng nhập sai mật khẩu -> từ chối, AppSession vẫn là GUEST
            assertThrows(ValidationException.class, () -> authService.login(
                    new LoginRequest(dupEmail, "SaiMatKhau999".toCharArray())
            ));
            assertFalse(session.isLoggedIn());

            // Giả lập người dùng đã có giỏ hàng total_quantity = 3 trong DB
            Cart cart = cartDAO.getOrCreateByUserId(createdUser.getId());
            try (Connection c = cf.getConnection();
                 PreparedStatement ps = c.prepareStatement("UPDATE carts SET total_quantity = 3 WHERE id = ?")) {
                ps.setLong(1, cart.getId());
                ps.executeUpdate();
            }

            // Đăng nhập đúng -> tạo UserSession + đồng bộ cartQuantity = 3
            UserSession loggedIn = authService.login(new LoginRequest(dupEmail, "MatKhauManh123".toCharArray()));
            assertTrue(session.isLoggedIn());
            assertTrue(loggedIn.isUser());
            assertEquals(3, session.getCartQuantity());

            // Đăng xuất -> xóa phiên RAM & đưa badge về 0, nhưng DB vẫn giữ total_quantity = 3
            authService.logout();
            assertFalse(session.isLoggedIn());
            assertEquals(0, session.getCartQuantity());
            assertEquals(3, cartDAO.findByUserId(createdUser.getId()).orElseThrow().getTotalQuantity());

            // Khóa tài khoản -> không cho đăng nhập
            userDAO.updateActiveStatus(createdUser.getId(), false);
            assertThrows(ValidationException.class, () -> authService.login(
                    new LoginRequest(dupEmail, "MatKhauManh123".toCharArray())
            ));
            assertFalse(session.isLoggedIn());
        } finally {
            if (createdUser != null && createdUser.getId() != null) {
                try (Connection c = cf.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM carts WHERE user_id = ?")) {
                        ps.setLong(1, createdUser.getId());
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
                        ps.setLong(1, createdUser.getId());
                        ps.executeUpdate();
                    }
                }
            }
        }
    }
}
