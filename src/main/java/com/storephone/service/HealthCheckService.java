package com.storephone.service;

import com.storephone.config.ConnectionFactory;
import com.storephone.dao.ProductDAO;
import com.storephone.dao.RoleDAO;
import com.storephone.dao.UserDAO;

public class HealthCheckService {
    public record StatusSnapshot(boolean connected, int roleCount, long userCount, long activeProductCount, String message) {
    }

    private final ConnectionFactory connectionFactory;
    private final RoleDAO roleDAO;
    private final UserDAO userDAO;
    private final ProductDAO productDAO;

    public HealthCheckService(
            ConnectionFactory connectionFactory,
            RoleDAO roleDAO,
            UserDAO userDAO,
            ProductDAO productDAO
    ) {
        this.connectionFactory = connectionFactory;
        this.roleDAO = roleDAO;
        this.userDAO = userDAO;
        this.productDAO = productDAO;
    }

    public StatusSnapshot checkStatus() {
        if (!connectionFactory.ping()) {
            return new StatusSnapshot(
                    false,
                    0,
                    0,
                    0,
                    "Chưa kết nối được MySQL — vui lòng kiểm tra MySQL Server và tệp config.properties."
            );
        }
        try {
            int roles = roleDAO.findAll().size();
            long users = userDAO.countAll();
            long products = productDAO.countAllActive();
            return new StatusSnapshot(
                    true,
                    roles,
                    users,
                    products,
                    String.format("MySQL sẵn sàng | Vai trò: %d | Người dùng: %d | Điện thoại đang bán: %d", roles, users, products)
            );
        } catch (RuntimeException ex) {
            return new StatusSnapshot(
                    true,
                    0,
                    0,
                    0,
                    "Đã kết nối MySQL nhưng chưa đủ bảng chuẩn — hãy chạy tệp sql/01_schema.sql."
            );
        }
    }
}
