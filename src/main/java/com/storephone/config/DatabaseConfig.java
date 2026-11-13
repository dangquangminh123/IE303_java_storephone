package com.storephone.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String DEFAULT_CONFIG_FILE = "config.properties";
    private static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3306/storephone_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_IMAGES_DIR = "data/images";

    private final String url;
    private final String username;
    private final String password;
    private final Path imagesDirectory;

    public DatabaseConfig(String url, String username, String password, Path imagesDirectory) {
        this.url = url;
        this.username = username;
        this.password = password == null ? "" : password;
        this.imagesDirectory = imagesDirectory == null ? Path.of(DEFAULT_IMAGES_DIR) : imagesDirectory;
    }

    public static DatabaseConfig load() {
        return loadFromPath(Path.of(DEFAULT_CONFIG_FILE));
    }

    public static DatabaseConfig loadFromPath(Path configPath) {
        Properties props = new Properties();
        if (configPath != null && Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            } catch (IOException ex) {
                throw new IllegalStateException("Không thể đọc tệp cấu hình: " + configPath.toAbsolutePath(), ex);
            }
        }

        String url = firstNonBlank(System.getenv("STOREPHONE_DB_URL"), props.getProperty("db.url"), DEFAULT_URL);
        String username = firstNonBlank(System.getenv("STOREPHONE_DB_USER"), props.getProperty("db.username"), DEFAULT_USER);
        String password = firstNonNull(System.getenv("STOREPHONE_DB_PASSWORD"), props.getProperty("db.password"), "");
        String imagesDir = firstNonBlank(System.getenv("STOREPHONE_IMAGES_DIR"), props.getProperty("app.images.dir"), DEFAULT_IMAGES_DIR);

        return new DatabaseConfig(url, username, password, Path.of(imagesDir));
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Path getImagesDirectory() {
        return imagesDirectory;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{url='" + url + "', username='" + username + "', password=[REDACTED]}";
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c.trim();
            }
        }
        return "";
    }

    private static String firstNonNull(String... candidates) {
        for (String c : candidates) {
            if (c != null) {
                return c;
            }
        }
        return "";
    }
}
