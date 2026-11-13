package com.storephone.service;

import com.storephone.dao.CartDAO;
import com.storephone.dao.RoleDAO;
import com.storephone.dao.UserDAO;
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
import com.storephone.util.InputValidator;
import java.util.Arrays;
import java.util.Objects;

public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserDAO userDAO;
    private final RoleDAO roleDAO;
    private final CartDAO cartDAO;
    private final AppSession appSession;

    public AuthService(UserDAO userDAO, RoleDAO roleDAO, CartDAO cartDAO, AppSession appSession) {
        this.userDAO = Objects.requireNonNull(userDAO);
        this.roleDAO = Objects.requireNonNull(roleDAO);
        this.cartDAO = Objects.requireNonNull(cartDAO);
        this.appSession = Objects.requireNonNull(appSession);
    }

    public User register(RegisterRequest request) {
        if (request == null) {
            throw new ValidationException("Dữ liệu đăng ký không hợp lệ.");
        }
        try {
            String fullName = InputValidator.requireNonBlank("fullName", request.getFullName(), "Họ và tên");
            String email = InputValidator.validateEmail(request.getEmail());
            String phone = InputValidator.validatePhone(request.getPhone(), false);
            String address = request.getAddress() == null || request.getAddress().trim().isEmpty()
                    ? null
                    : request.getAddress().trim();

            String rawPassword = new String(request.getPassword());
            InputValidator.validatePassword(rawPassword, MIN_PASSWORD_LENGTH);

            if (!Arrays.equals(request.getPassword(), request.getConfirmPassword())) {
                throw new ValidationException("confirmPassword", "Mật khẩu xác nhận không khớp.");
            }

            if (userDAO.existsByEmail(email)) {
                throw new DuplicateEmailException("Email '" + email + "' đã được sử dụng.");
            }

            Role userRole = roleDAO.findByName(Role.USER)
                    .orElseThrow(() -> new IllegalStateException("Chưa khởi tạo vai trò USER trong CSDL."));

            User newUser = new User();
            newUser.setRoleId(userRole.getId());
            newUser.setEmail(email);
            newUser.setPasswordHash(PasswordHasher.hash(rawPassword));
            newUser.setFullName(fullName);
            newUser.setPhone(phone);
            newUser.setAddress(address);
            newUser.setActive(true);

            User saved = userDAO.insert(newUser);
            cartDAO.getOrCreateByUserId(saved.getId());
            return saved;
        } finally {
            request.clearSensitive();
        }
    }

    public UserSession login(LoginRequest request) {
        if (request == null) {
            throw new ValidationException("Dữ liệu đăng nhập không hợp lệ.");
        }
        try {
            String email = InputValidator.validateEmail(request.getEmail());
            String rawPassword = new String(request.getPassword());
            if (rawPassword.isBlank()) {
                throw new ValidationException("password", "Mật khẩu không được để trống.");
            }

            User user = userDAO.findByEmail(email)
                    .orElseThrow(() -> new ValidationException("auth", "Email hoặc mật khẩu không chính xác."));

            if (!PasswordHasher.verify(rawPassword, user.getPasswordHash())) {
                throw new ValidationException("auth", "Email hoặc mật khẩu không chính xác.");
            }

            if (!user.isActive()) {
                throw new ValidationException("auth", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
            }

            int cartQty = cartDAO.findByUserId(user.getId())
                    .map(Cart::getTotalQuantity)
                    .orElse(0);

            UserSession session = new UserSession(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRoleName()
            );
            appSession.login(session, cartQty);
            return session;
        } finally {
            request.clearSensitive();
        }
    }

    public void logout() {
        appSession.logout();
    }
}
