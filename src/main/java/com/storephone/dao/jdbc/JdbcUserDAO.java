package com.storephone.dao.jdbc;

import com.storephone.config.ConnectionFactory;
import com.storephone.dao.UserDAO;
import com.storephone.dto.PageResult;
import com.storephone.exception.DataAccessException;
import com.storephone.exception.DuplicateEmailException;
import com.storephone.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserDAO implements UserDAO {
    private static final String BASE_SELECT = """
            SELECT u.id, u.role_id, r.name AS role_name, u.email, u.password_hash,
                   u.full_name, u.phone, u.address, u.avatar_path, u.active, u.created_at
            FROM users u JOIN roles r ON r.id = u.role_id
            """;

    private final ConnectionFactory cf;

    public JdbcUserDAO(ConnectionFactory connectionFactory) {
        this.cf = connectionFactory;
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + " WHERE u.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(JdbcRowMapper.mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi truy vấn người dùng theo ID.", ex);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + " WHERE LOWER(u.email) = LOWER(?)")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(JdbcRowMapper.mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi truy vấn người dùng theo email.", ex);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi kiểm tra email tồn tại.", ex);
        }
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long excludeUserId) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) AND id <> ? LIMIT 1")) {
            ps.setString(1, email);
            ps.setLong(2, excludeUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi kiểm tra trùng email.", ex);
        }
    }

    @Override
    public PageResult<User> findPage(String keyword, int page, int pageSize) {
        int p = Math.max(1, page);
        int sz = Math.max(1, pageSize);
        boolean hasKw = keyword != null && !keyword.trim().isEmpty();
        String where = hasKw ? " WHERE LOWER(u.email) LIKE ? OR LOWER(u.full_name) LIKE ?" : "";

        try (Connection c = cf.getConnection()) {
            long total;
            try (PreparedStatement cps = c.prepareStatement("SELECT COUNT(*) FROM users u" + where)) {
                if (hasKw) {
                    String pat = "%" + keyword.trim().toLowerCase() + "%";
                    cps.setString(1, pat);
                    cps.setString(2, pat);
                }
                try (ResultSet rs = cps.executeQuery()) {
                    total = rs.next() ? rs.getLong(1) : 0L;
                }
            }
            List<User> list = new ArrayList<>();
            try (PreparedStatement dps = c.prepareStatement(BASE_SELECT + where + " ORDER BY u.id DESC LIMIT ? OFFSET ?")) {
                int i = 1;
                if (hasKw) {
                    String pat = "%" + keyword.trim().toLowerCase() + "%";
                    dps.setString(i++, pat);
                    dps.setString(i++, pat);
                }
                dps.setInt(i++, sz);
                dps.setInt(i, (p - 1) * sz);
                try (ResultSet rs = dps.executeQuery()) {
                    while (rs.next()) {
                        list.add(JdbcRowMapper.mapUser(rs));
                    }
                }
            }
            return new PageResult<>(list, total, p, sz);
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi phân trang người dùng.", ex);
        }
    }

    @Override
    public long countAll() {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi đếm người dùng.", ex);
        }
    }

    @Override
    public long countActive() {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users WHERE active = TRUE");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi đếm người dùng đang hoạt động.", ex);
        }
    }

    @Override
    public long countActiveAdmins() {
        String sql = """
                SELECT COUNT(*)
                FROM users u JOIN roles r ON r.id = u.role_id
                WHERE u.active = TRUE AND UPPER(r.name) = 'ADMIN'
                """;
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi đếm quản trị viên đang hoạt động.", ex);
        }
    }

    @Override
    public User insert(User user) {
        String sql = "INSERT INTO users (role_id, email, password_hash, full_name, phone, address, avatar_path, active) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, user.getRoleId());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getAddress());
            ps.setString(7, user.getAvatarPath());
            ps.setBoolean(8, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return findById(user.getId()).orElse(user);
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                throw new DuplicateEmailException();
            }
            throw new DataAccessException("Lỗi thêm mới người dùng.", ex);
        }
    }

    @Override
    public void updateProfile(User u) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET full_name=?, phone=?, address=?, avatar_path=? WHERE id=?")) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getPhone());
            ps.setString(3, u.getAddress());
            ps.setString(4, u.getAvatarPath());
            ps.setLong(5, u.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi cập nhật hồ sơ cá nhân.", ex);
        }
    }

    @Override
    public void updateAdminFields(User u) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET role_id=?, full_name=?, phone=?, address=?, avatar_path=?, active=? WHERE id=?")) {
            ps.setLong(1, u.getRoleId());
            ps.setString(2, u.getFullName());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getAddress());
            ps.setString(5, u.getAvatarPath());
            ps.setBoolean(6, u.isActive());
            ps.setLong(7, u.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi cập nhật người dùng (Admin).", ex);
        }
    }

    @Override
    public void updateActiveStatus(Long userId, boolean active) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET active=? WHERE id=?")) {
            ps.setBoolean(1, active);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi khóa/mở khóa người dùng.", ex);
        }
    }
}
