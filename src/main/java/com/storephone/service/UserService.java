package com.storephone.service;

import com.storephone.dao.CartDAO;
import com.storephone.dao.RoleDAO;
import com.storephone.dao.UserDAO;
import com.storephone.dto.PageResult;
import com.storephone.dto.ProfileUpdateRequest;
import com.storephone.dto.UserAdminUpdateRequest;
import com.storephone.dto.UserCreateRequest;
import com.storephone.dto.UserSession;
import com.storephone.exception.DuplicateEmailException;
import com.storephone.exception.ResourceNotFoundException;
import com.storephone.exception.ValidationException;
import com.storephone.model.Role;
import com.storephone.model.User;
import com.storephone.security.AppSession;
import com.storephone.security.PasswordHasher;
import com.storephone.util.ImageStorage;
import com.storephone.util.InputValidator;
import java.util.List;
import java.util.Objects;

public class UserService {
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserDAO userDAO;
    private final RoleDAO roleDAO;
    private final CartDAO cartDAO;
    private final ImageStorage imageStorage;
    private final AppSession appSession;
    private final AuthorizationService authorizationService;

    public UserService(
            UserDAO userDAO,
            RoleDAO roleDAO,
            CartDAO cartDAO,
            ImageStorage imageStorage,
            AppSession appSession,
            AuthorizationService authorizationService
    ) {
        this.userDAO = Objects.requireNonNull(userDAO);
        this.roleDAO = Objects.requireNonNull(roleDAO);
        this.cartDAO = Objects.requireNonNull(cartDAO);
        this.imageStorage = Objects.requireNonNull(imageStorage);
        this.appSession = Objects.requireNonNull(appSession);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public PageResult<User> searchUsersForAdmin(String keyword, int page, int pageSize) {
        authorizationService.requireAdmin();
        PageResult<User> raw = userDAO.findPage(keyword, page, pageSize);
        List<User> safeList = raw.getItems().stream().map(UserService::scrubPassword).toList();
        return new PageResult<>(safeList, raw.getTotalElements(), raw.getPage(), raw.getPageSize());
    }

    public User getUserDetailForAdmin(Long userId) {
        authorizationService.requireAdmin();
        if (userId == null) {
            throw new ValidationException("ID người dùng không hợp lệ.");
        }
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID = " + userId));
        return scrubPassword(user);
    }

    public List<Role> getAllRoles() {
        authorizationService.requireAdmin();
        return roleDAO.findAll();
    }

    public User createUserByAdmin(UserCreateRequest request) {
        authorizationService.requireAdmin();
        if (request == null) {
            throw new ValidationException("Dữ liệu tạo người dùng không hợp lệ.");
        }
        String storedAvatar = null;
        try {
            String email = InputValidator.validateEmail(request.getEmail());
            String fullName = InputValidator.requireNonBlank("fullName", request.getFullName(), "Họ và tên");
            String phone = InputValidator.validatePhone(request.getPhone(), false);
            String address = normalizeOptional(request.getAddress());
            String rawPassword = new String(request.getPassword());
            InputValidator.validatePassword(rawPassword, MIN_PASSWORD_LENGTH);

            String roleName = InputValidator.requireNonBlank("role", request.getRoleName(), "Vai trò").toUpperCase();
            Role role = roleDAO.findByName(roleName)
                    .orElseThrow(() -> new ValidationException("role", "Vai trò không hợp lệ: " + roleName));

            if (userDAO.existsByEmail(email)) {
                throw new DuplicateEmailException("Email '" + email + "' đã được sử dụng.");
            }
            if (request.getAvatarSourceFile() != null) {
                storedAvatar = imageStorage.saveImage(request.getAvatarSourceFile(), "avatars");
            }

            User user = new User();
            user.setRoleId(role.getId());
            user.setEmail(email);
            user.setPasswordHash(PasswordHasher.hash(rawPassword));
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setAddress(address);
            user.setAvatarPath(storedAvatar);
            user.setActive(request.isActive());

            User saved = userDAO.insert(user);
            if (Role.USER.equalsIgnoreCase(role.getName())) {
                cartDAO.getOrCreateByUserId(saved.getId());
            }
            return scrubPassword(saved);
        } catch (RuntimeException ex) {
            if (storedAvatar != null) {
                imageStorage.deleteQuietly(storedAvatar);
            }
            throw ex;
        } finally {
            request.clearSensitive();
        }
    }

    public User updateUserByAdmin(UserAdminUpdateRequest request) {
        authorizationService.requireAdmin();
        if (request == null || request.userId() == null) {
            throw new ValidationException("Dữ liệu cập nhật người dùng không hợp lệ.");
        }
        User existing = userDAO.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID = " + request.userId()));

        UserSession currentAdmin = appSession.getCurrentUser().orElseThrow();
        String fullName = InputValidator.requireNonBlank("fullName", request.fullName(), "Họ và tên");
        String phone = InputValidator.validatePhone(request.phone(), false);
        String address = normalizeOptional(request.address());
        String targetRoleName = InputValidator.requireNonBlank("role", request.roleName(), "Vai trò").toUpperCase();

        Role targetRole = roleDAO.findByName(targetRoleName)
                .orElseThrow(() -> new ValidationException("role", "Vai trò không hợp lệ: " + targetRoleName));

        if (existing.getId().equals(currentAdmin.userId())) {
            if (!request.active()) {
                throw new ValidationException("active", "Quản trị viên không thể tự khóa tài khoản của chính mình.");
            }
            if (!Role.ADMIN.equalsIgnoreCase(targetRole.getName())) {
                throw new ValidationException("role", "Quản trị viên không thể tự hạ quyền của chính mình.");
            }
        }

        boolean wasActiveAdmin = existing.isActive() && Role.ADMIN.equalsIgnoreCase(existing.getRoleName());
        boolean willRemainActiveAdmin = request.active() && Role.ADMIN.equalsIgnoreCase(targetRole.getName());
        if (wasActiveAdmin && !willRemainActiveAdmin && userDAO.countActiveAdmins() <= 1) {
            throw new ValidationException("role", "Không thể khóa hoặc hạ quyền tài khoản ADMIN hoạt động cuối cùng.");
        }

        String oldAvatar = existing.getAvatarPath();
        String newAvatar = null;
        try {
            if (request.newAvatarSourceFile() != null) {
                newAvatar = imageStorage.saveImage(request.newAvatarSourceFile(), "avatars");
            }
            existing.setRoleId(targetRole.getId());
            existing.setFullName(fullName);
            existing.setPhone(phone);
            existing.setAddress(address);
            if (newAvatar != null) {
                existing.setAvatarPath(newAvatar);
            }
            existing.setActive(request.active());

            userDAO.updateAdminFields(existing);
            if (newAvatar != null && oldAvatar != null && !oldAvatar.equals(newAvatar)) {
                imageStorage.deleteQuietly(oldAvatar);
            }
            if (existing.getId().equals(currentAdmin.userId())) {
                appSession.updateCurrentUserProfile(fullName);
            }
            return getUserDetailForAdmin(existing.getId());
        } catch (RuntimeException ex) {
            if (newAvatar != null) {
                imageStorage.deleteQuietly(newAvatar);
            }
            throw ex;
        }
    }

    public void setUserActiveStatus(Long targetUserId, boolean active) {
        authorizationService.requireAdmin();
        if (targetUserId == null) {
            throw new ValidationException("ID người dùng không hợp lệ.");
        }
        User target = userDAO.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID = " + targetUserId));

        UserSession currentAdmin = appSession.getCurrentUser().orElseThrow();
        if (!active && target.getId().equals(currentAdmin.userId())) {
            throw new ValidationException("active", "Quản trị viên không thể tự khóa tài khoản của chính mình.");
        }
        if (!active && target.isActive() && Role.ADMIN.equalsIgnoreCase(target.getRoleName())
                && userDAO.countActiveAdmins() <= 1) {
            throw new ValidationException("active", "Không thể khóa tài khoản ADMIN hoạt động cuối cùng.");
        }
        userDAO.updateActiveStatus(targetUserId, active);
    }

    public User getCurrentUserProfile() {
        authorizationService.requireAuthenticated();
        Long userId = appSession.getCurrentUser().orElseThrow().userId();
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ người dùng hiện tại."));
        return scrubPassword(user);
    }

    public User updateCurrentUserProfile(ProfileUpdateRequest request) {
        authorizationService.requireAuthenticated();
        if (request == null) {
            throw new ValidationException("Dữ liệu cập nhật hồ sơ không hợp lệ.");
        }
        Long userId = appSession.getCurrentUser().orElseThrow().userId();
        User existing = userDAO.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ người dùng hiện tại."));

        String fullName = InputValidator.requireNonBlank("fullName", request.fullName(), "Họ và tên");
        String phone = InputValidator.validatePhone(request.phone(), false);
        String address = normalizeOptional(request.address());

        String oldAvatar = existing.getAvatarPath();
        String newAvatar = null;
        try {
            if (request.newAvatarSourceFile() != null) {
                newAvatar = imageStorage.saveImage(request.newAvatarSourceFile(), "avatars");
            }
            existing.setFullName(fullName);
            existing.setPhone(phone);
            existing.setAddress(address);
            if (newAvatar != null) {
                existing.setAvatarPath(newAvatar);
            }
            userDAO.updateProfile(existing);
            if (newAvatar != null && oldAvatar != null && !oldAvatar.equals(newAvatar)) {
                imageStorage.deleteQuietly(oldAvatar);
            }
            appSession.updateCurrentUserProfile(fullName);
            return getCurrentUserProfile();
        } catch (RuntimeException ex) {
            if (newAvatar != null) {
                imageStorage.deleteQuietly(newAvatar);
            }
            throw ex;
        }
    }

    private static String normalizeOptional(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static User scrubPassword(User source) {
        source.setPasswordHash(null);
        return source;
    }
}


