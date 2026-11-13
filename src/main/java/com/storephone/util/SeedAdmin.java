package com.storephone.util;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import com.storephone.dao.RoleDAO;
import com.storephone.dao.UserDAO;
import com.storephone.dao.jdbc.JdbcRoleDAO;
import com.storephone.dao.jdbc.JdbcUserDAO;
import com.storephone.model.Role;
import com.storephone.model.User;
import com.storephone.security.PasswordHasher;

/**
 * Tiện ích khởi tạo tài khoản ADMIN đầu tiên qua biến môi trường (mã hóa BCrypt, không hardcode mật khẩu).
 * Chạy: STOREPHONE_ADMIN_EMAIL=... STOREPHONE_ADMIN_PASSWORD=... ./mvnw compile exec:java -Dexec.mainClass="com.storephone.util.SeedAdmin"
 */
public final class SeedAdmin {
    private SeedAdmin() {
    }

    public static void main(String[] args) {
        String email = System.getenv("STOREPHONE_ADMIN_EMAIL");
        String password = System.getenv("STOREPHONE_ADMIN_PASSWORD");
        String fullName = System.getenv().getOrDefault("STOREPHONE_ADMIN_NAME", "Quản trị viên StorePhone");

        String validEmail = InputValidator.validateEmail(email);
        InputValidator.validatePassword(password, 8);

        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        RoleDAO roleDAO = new JdbcRoleDAO(cf);
        UserDAO userDAO = new JdbcUserDAO(cf);

        Role adminRole = roleDAO.findByName(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Chưa có vai trò ADMIN trong bảng roles. Hãy chạy sql/01_schema.sql trước."));

        if (userDAO.existsByEmail(validEmail)) {
            System.out.println("Tài khoản ADMIN đã tồn tại: " + validEmail);
            return;
        }

        User admin = new User();
        admin.setRoleId(adminRole.getId());
        admin.setEmail(validEmail);
        admin.setPasswordHash(PasswordHasher.hash(password));
        admin.setFullName(fullName.trim());
        admin.setActive(true);

        User saved = userDAO.insert(admin);
        System.out.println("Đã khởi tạo tài khoản ADMIN thành công (ID=" + saved.getId() + ", Email=" + saved.getEmail() + ").");
    }
}
