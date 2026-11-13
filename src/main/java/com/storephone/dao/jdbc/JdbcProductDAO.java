package com.storephone.dao.jdbc;

import com.storephone.config.ConnectionFactory;
import com.storephone.dao.ProductDAO;
import com.storephone.dto.PageResult;
import com.storephone.dto.ProductFilterCriteria;
import com.storephone.exception.DataAccessException;
import com.storephone.model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class JdbcProductDAO implements ProductDAO {
    private static final String BASE_SELECT = """
            SELECT id, name, price, short_desc, detail_desc, quantity,
                   factory, target, image_path, active, created_at, updated_at
            FROM products
            """;

    private final ConnectionFactory cf;

    public JdbcProductDAO(ConnectionFactory connectionFactory) {
        this.cf = connectionFactory;
    }

    @Override
    public Optional<Product> findById(Long id) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + " WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(JdbcRowMapper.mapProduct(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi truy vấn sản phẩm theo ID.", ex);
        }
    }

    @Override
    public Optional<Product> findByIdForUpdate(Connection conn, Long id) {
        try (PreparedStatement ps = conn.prepareStatement(BASE_SELECT + " WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(JdbcRowMapper.mapProduct(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi khóa sản phẩm khi đặt hàng.", ex);
        }
    }

    @Override
    public PageResult<Product> search(ProductFilterCriteria criteria) {
        ProductFilterCriteria q = criteria == null ? new ProductFilterCriteria() : criteria;
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (!q.isIncludeInactive()) {
            where.append(" AND active = TRUE");
        }
        if (q.getKeyword() != null && !q.getKeyword().trim().isEmpty()) {
            where.append(" AND LOWER(name) LIKE ?");
            params.add("%" + q.getKeyword().trim().toLowerCase() + "%");
        }
        if (!q.getFactories().isEmpty()) {
            where.append(" AND factory IN (")
                    .append(String.join(",", Collections.nCopies(q.getFactories().size(), "?"))).append(")");
            params.addAll(q.getFactories());
        }
        if (!q.getTargets().isEmpty()) {
            where.append(" AND target IN (")
                    .append(String.join(",", Collections.nCopies(q.getTargets().size(), "?"))).append(")");
            params.addAll(q.getTargets());
        }
        if (!q.getPriceRanges().isEmpty()) {
            List<String> orParts = new ArrayList<>();
            for (ProductFilterCriteria.PriceRange range : q.getPriceRanges()) {
                if (range.min() != null && range.max() != null) {
                    orParts.add("(price >= ? AND price <= ?)");
                    params.add(range.min());
                    params.add(range.max());
                } else if (range.min() != null) {
                    orParts.add("(price >= ?)");
                    params.add(range.min());
                } else if (range.max() != null) {
                    orParts.add("(price <= ?)");
                    params.add(range.max());
                }
            }
            if (!orParts.isEmpty()) {
                where.append(" AND (").append(String.join(" OR ", orParts)).append(")");
            }
        }
        if (q.getMinPrice() != null) {
            where.append(" AND price >= ?");
            params.add(q.getMinPrice());
        }
        if (q.getMaxPrice() != null) {
            where.append(" AND price <= ?");
            params.add(q.getMaxPrice());
        }

        String dataSql = BASE_SELECT + where + " ORDER BY " + q.getSortBy().getOrderByClause() + " LIMIT ? OFFSET ?";
        try (Connection c = cf.getConnection()) {
            long total;
            try (PreparedStatement cps = c.prepareStatement("SELECT COUNT(*) FROM products" + where)) {
                bind(cps, params);
                try (ResultSet rs = cps.executeQuery()) {
                    total = rs.next() ? rs.getLong(1) : 0L;
                }
            }
            List<Product> items = new ArrayList<>();
            try (PreparedStatement dps = c.prepareStatement(dataSql)) {
                int idx = bind(dps, params);
                dps.setInt(idx++, q.getPageSize());
                dps.setInt(idx, q.getOffset());
                try (ResultSet rs = dps.executeQuery()) {
                    while (rs.next()) {
                        items.add(JdbcRowMapper.mapProduct(rs));
                    }
                }
            }
            return new PageResult<>(items, total, q.getPage(), q.getPageSize());
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi tìm kiếm sản phẩm.", ex);
        }
    }

    @Override
    public List<String> findDistinctFactories() {
        List<String> list = new ArrayList<>();
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT DISTINCT factory FROM products WHERE active = TRUE ORDER BY factory ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi lấy danh sách hãng.", ex);
        }
    }

    @Override
    public long countAllActive() {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM products WHERE active = TRUE");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi đếm sản phẩm.", ex);
        }
    }

    @Override
    public Product insert(Product p) {
        String sql = "INSERT INTO products (name, price, short_desc, detail_desc, quantity, factory, target, image_path, active) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setBigDecimal(2, p.getPrice());
            ps.setString(3, p.getShortDesc());
            ps.setString(4, p.getDetailDesc());
            ps.setInt(5, p.getQuantity());
            ps.setString(6, p.getFactory());
            ps.setString(7, p.getTarget());
            ps.setString(8, p.getImagePath());
            ps.setBoolean(9, p.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getLong(1));
                }
            }
            return findById(p.getId()).orElse(p);
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi thêm mới sản phẩm.", ex);
        }
    }

    @Override
    public void update(Product p) {
        String sql = "UPDATE products SET name=?, price=?, short_desc=?, detail_desc=?, quantity=?, factory=?, target=?, image_path=?, active=? WHERE id=?";
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setBigDecimal(2, p.getPrice());
            ps.setString(3, p.getShortDesc());
            ps.setString(4, p.getDetailDesc());
            ps.setInt(5, p.getQuantity());
            ps.setString(6, p.getFactory());
            ps.setString(7, p.getTarget());
            ps.setString(8, p.getImagePath());
            ps.setBoolean(9, p.isActive());
            ps.setLong(10, p.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi cập nhật sản phẩm.", ex);
        }
    }

    @Override
    public void updateActiveStatus(Long productId, boolean active) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE products SET active=? WHERE id=?")) {
            ps.setBoolean(1, active);
            ps.setLong(2, productId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi cập nhật trạng thái sản phẩm.", ex);
        }
    }

    @Override
    public boolean deductStock(Connection conn, Long productId, int qty) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET quantity = quantity - ? WHERE id = ? AND active = TRUE AND quantity >= ?")) {
            ps.setInt(1, qty);
            ps.setLong(2, productId);
            ps.setInt(3, qty);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi trừ tồn kho.", ex);
        }
    }

    @Override
    public void restoreStock(Connection conn, Long productId, int qty) {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE products SET quantity = quantity + ? WHERE id = ?")) {
            ps.setInt(1, qty);
            ps.setLong(2, productId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi hoàn tồn kho.", ex);
        }
    }

    @Override
    public boolean isReferencedInCartOrOrders(Long productId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM cart_items WHERE product_id=?) OR EXISTS(SELECT 1 FROM order_items WHERE product_id=?)";
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setLong(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi kiểm tra ràng buộc sản phẩm.", ex);
        }
    }

    @Override
    public void deleteById(Long productId) {
        try (Connection c = cf.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id=?")) {
            ps.setLong(1, productId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Lỗi xóa sản phẩm.", ex);
        }
    }

    private static int bind(PreparedStatement ps, List<Object> params) throws SQLException {
        int i = 1;
        for (Object param : params) {
            ps.setObject(i++, param);
        }
        return i;
    }
}
