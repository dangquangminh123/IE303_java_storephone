package com.storephone.util;

import com.storephone.config.ConnectionFactory;
import com.storephone.config.DatabaseConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Tiện ích thực thi tệp SQL khởi tạo bảng & dữ liệu mẫu trực tiếp qua JDBC (không phụ thuộc CLI mysql).
 */
public final class DatabaseInitializer {
    private static volatile boolean schemaInitializedInProcess = false;

    private DatabaseInitializer() {
    }

    public static void main(String[] args) throws Exception {
        ConnectionFactory cf = new ConnectionFactory(DatabaseConfig.load());
        boolean seedProducts = args.length > 0 && "--with-sample-products".equals(args[0]);
        initializeSchema(cf, seedProducts);
        System.out.println("Đã khởi tạo lược đồ CSDL thành công.");
    }

    public static void initializeSchema(ConnectionFactory cf, boolean seedSampleProducts) throws IOException, java.sql.SQLException {
        if (!schemaInitializedInProcess) {
            executeSqlScript(cf, Path.of("sql", "01_schema.sql"));
            schemaInitializedInProcess = true;
        }
        if (seedSampleProducts && isProductTableEmpty(cf)) {
            executeSqlScript(cf, Path.of("sql", "02_seed_sample_products.sql"));
        }
    }

    private static boolean isProductTableEmpty(ConnectionFactory cf) throws java.sql.SQLException {
        try (Connection c = cf.getConnection();
             Statement st = c.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM products")) {
            return !rs.next() || rs.getLong(1) == 0L;
        }
    }

    public static void executeSqlScript(ConnectionFactory cf, Path sqlPath) throws IOException, java.sql.SQLException {
        String content = Files.readString(sqlPath, StandardCharsets.UTF_8);
        StringBuilder withoutComments = new StringBuilder();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            withoutComments.append(line).append('\n');
        }

        try (Connection conn = cf.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String rawStatement : withoutComments.toString().split(";")) {
                String sql = rawStatement.trim();
                String upper = sql.toUpperCase();
                if (sql.isEmpty() || upper.startsWith("CREATE DATABASE") || upper.startsWith("USE ")) {
                    continue;
                }
                stmt.execute(sql);
            }
        }
    }
}
