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
import com.storephone.dto.ProfileUpdateRequest;
import com.storephone.dto.UserAdminUpdateRequest;
import com.storephone.dto.UserCreateRequest;
import com.storephone.dto.UserSession;
import com.storephone.exception.AuthorizationException;
import com.storephone.exception.ValidationException;
import com.storephone.model.Role;
import com.storephone.model.User;
import com.storephone.security.AppSession;
import com.storephone.security.PasswordHasher;
import com.storephone.service.AuthorizationService;
import com.storephone.service.UserService;
import com.storephone.util.DatabaseInitializer;
import com.storephone.util.ImageStorage;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserServiceTest {

    @Test
    void adminShouldManageUsersWithAvatarAndEnforceGuards(@TempDir Path tempDir) throws Exception {
        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        assumeTrue(cf.ping(), "Bỏ qua khi MySQL chưa khởi chạy.");
        DatabaseInitializer.initializeSchema(cf, false);

        RoleDAO roleDAO = new JdbcRoleDAO(cf);
        UserDAO userDAO = new JdbcUserDAO(cf);
        CartDAO cartDAO = new JdbcCartDAO(cf);
        ImageStorage imageStorage = new ImageStorage(tempDir.resolve("images"));
        AppSession session = new AppSession();
        UserService userService = new UserService(
                userDAO, roleDAO, cartDAO, imageStorage, session, new AuthorizationService(session));

        Role adminRole = roleDAO.findByName(Role.ADMIN).orElseThrow();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User admin = new User();
        admin.setRoleId(adminRole.getId());
        admin.setEmail("admin_" + suffix + "@storephone.vn");
        admin.setPasswordHash(PasswordHasher.hash("AdminPass123"));
        admin.setFullName("Quản Trị Viên Test");
        admin.setActive(true);
        User savedAdmin = userDAO.insert(admin);
        User createdUser = null;

        try {
            session.login(new UserSession(999L, "u@test.vn", "User", Role.USER));
            assertThrows(AuthorizationException.class, () -> userService.searchUsersForAdmin(null, 1, 10));

            session.login(new UserSession(savedAdmin.getId(), savedAdmin.getEmail(), savedAdmin.getFullName(), Role.ADMIN));
            Path avatarSrc = tempDir.resolve("avatar.png");
            ImageIO.write(new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB), "png", avatarSrc.toFile());

            createdUser = userService.createUserByAdmin(new UserCreateRequest(
                    "staff_" + suffix + "@storephone.vn", "MatKhauStaff123".toCharArray(),
                    "Trần Thị B", "0901234567", "Quận 3", Role.USER, avatarSrc, true));
            assertNull(createdUser.getPasswordHash());
            assertTrue(createdUser.getAvatarPath().startsWith("avatars/"));
            assertTrue(Files.isRegularFile(imageStorage.resolvePath(createdUser.getAvatarPath())));

            String originalHash = userDAO.findById(createdUser.getId()).orElseThrow().getPasswordHash();
            User updated = userService.updateUserByAdmin(new UserAdminUpdateRequest(
                    createdUser.getId(), "Trần Thị B (VIP)", "0909998887", "Quận 1", Role.USER, null, true));
            assertEquals("Trần Thị B (VIP)", updated.getFullName());
            assertEquals(originalHash, userDAO.findById(createdUser.getId()).orElseThrow().getPasswordHash());

            assertThrows(ValidationException.class, () -> userService.setUserActiveStatus(savedAdmin.getId(), false));
            assertThrows(ValidationException.class, () -> userService.updateUserByAdmin(
                    new UserAdminUpdateRequest(savedAdmin.getId(), "Admin", null, null, Role.USER, null, true)));

            userService.setUserActiveStatus(createdUser.getId(), false);
            assertFalse(userDAO.findById(createdUser.getId()).orElseThrow().isActive());

            userService.updateCurrentUserProfile(new ProfileUpdateRequest("Admin Mới", "0911222333", "Hà Nội", null));
            assertEquals("Admin Mới", session.getCurrentUser().orElseThrow().fullName());
        } finally {
            try (Connection c = cf.getConnection()) {
                if (createdUser != null && createdUser.getId() != null) {
                    exec(c, "DELETE FROM carts WHERE user_id = ?", createdUser.getId());
                    exec(c, "DELETE FROM users WHERE id = ?", createdUser.getId());
                }
                if (savedAdmin.getId() != null) {
                    exec(c, "DELETE FROM users WHERE id = ?", savedAdmin.getId());
                }
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
