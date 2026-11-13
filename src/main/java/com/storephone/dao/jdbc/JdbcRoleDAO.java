package com.storephone.dao.jdbc;

import com.storephone.config.ConnectionFactory;
import com.storephone.dao.RoleDAO;
import com.storephone.exception.DataAccessException;
import com.storephone.model.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcRoleDAO implements RoleDAO {
    private final ConnectionFactory connectionFactory;

    public JdbcRoleDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Optional<Role> findById(Long id) {
        String sql = "SELECT id, name, description FROM roles WHERE id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi truy vấn vai trò theo ID.", ex);
        }
    }

    @Override
    public Optional<Role> findByName(String name) {
        String sql = "SELECT id, name, description FROM roles WHERE name = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi truy vấn vai trò theo tên.", ex);
        }
    }

    @Override
    public List<Role> findAll() {
        String sql = "SELECT id, name, description FROM roles ORDER BY id ASC";
        List<Role> roles = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(mapRow(rs));
            }
            return roles;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi lấy danh sách vai trò.", ex);
        }
    }

    private static Role mapRow(ResultSet rs) throws SQLException {
        return new Role(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description")
        );
    }
}
