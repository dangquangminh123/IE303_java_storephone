# BỘ MẪU KHỞI ĐỘNG (TÙY CHỌN) — KHÔNG PHẢI SOURCE ĐẦY ĐỦ

> Đọc sau `GIAI_DOAN_01` và `GIAI_DOAN_02`. Đây là **mẫu code mới** minh họa cách chuyển kiến trúc; không phải source do GitHub repository cung cấp. Phiên bản dependency dưới đây được **ghim cố định để làm mẫu**, không khẳng định là phiên bản mới nhất.

## 1. `pom.xml` Java 17 + Swing + JDBC (không Spring)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.storephone</groupId>
    <artifactId>storephone-swing</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.4.0</version>
        </dependency>
        <dependency>
            <groupId>org.mindrot</groupId>
            <artifactId>jbcrypt</artifactId>
            <version>0.4</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.11.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```
Java Swing là thành phần JDK, **không phải dependency Maven riêng**. Chưa thiết lập fat JAR; đọc giai đoạn 12 khi đóng gói.

## 2. `Main.java` dựng JFrame đầu tiên
```java
package com.storephone;

import javax.swing.*;
import java.awt.*;

public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("StorePhone - Java Swing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 700);
            frame.setLocationRelativeTo(null);

            JPanel pages = new JPanel(new CardLayout());
            pages.add(new JLabel("Danh sách điện thoại", SwingConstants.CENTER), "HOME");
            pages.add(new JLabel("Đăng nhập", SwingConstants.CENTER), "LOGIN");

            JPanel bar = new JPanel();
            JButton home = new JButton("Trang chủ");
            JButton login = new JButton("Đăng nhập");
            bar.add(home);
            bar.add(login);

            CardLayout layout = (CardLayout) pages.getLayout();
            home.addActionListener(e -> layout.show(pages, "HOME"));
            login.addActionListener(e -> layout.show(pages, "LOGIN"));

            frame.add(bar, BorderLayout.NORTH);
            frame.add(pages, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }
}
```
Chạy class này trước; sau khi có `MainFrame`, thay code demo bằng cấu trúc thực ở `02_KIEN_TRUC_MVC_SWING.md`.

## 3. `ConnectionFactory.java` không hard-code mật khẩu
```java
package com.storephone.config;

import java.sql.*;

public final class ConnectionFactory {
    public Connection getConnection() throws SQLException {
        String url = System.getenv("STOREPHONE_DB_URL");
        String user = System.getenv("STOREPHONE_DB_USER");
        String password = System.getenv("STOREPHONE_DB_PASSWORD");
        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Thiếu biến môi trường kết nối MySQL");
        }
        return DriverManager.getConnection(url, user, password);
    }
}
```
Ví dụ URL: `jdbc:mysql://localhost:3306/storephone_db`. Cấu hình password bằng environment hoặc file local nằm trong `.gitignore`. Không dùng root MySQL cho app nếu tạo được user chỉ có quyền cần thiết.

## 4. `JdbcProductDAO` — mẫu query an toàn
```java
package com.storephone.dao.jdbc;

import com.storephone.config.ConnectionFactory;
import java.sql.*;
import java.util.*;

public final class JdbcProductDAO {
    private final ConnectionFactory factory;
    public JdbcProductDAO(ConnectionFactory factory) {
        this.factory = factory;
    }

    public List<String> findNames(String keyword) throws SQLException {
        String sql = "SELECT name FROM products " +
                     "WHERE active=TRUE AND name LIKE ? ORDER BY id DESC";
        List<String> names = new ArrayList<>();
        try (Connection c = factory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString("name"));
            }
        }
        return names;
    }
}
```
Đây chỉ là bản minh họa đọc tên; sau đó thay `List<String>` bằng `List<Product>`/DTO có ID và thêm pagination, filter. **Không viết SQL trong lớp Swing.**

## 5. Mẫu BCrypt đúng cách
```java
import org.mindrot.jbcrypt.BCrypt;
String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
boolean ok = BCrypt.checkpw(rawPasswordInput, passwordHash);
```
Không in `rawPassword`, `passwordHash` vào console/log. Nên dùng `char[]` ở lớp UI; thư viện BCrypt minh họa nhận String nên phần dữ liệu String phát sinh cần giới hạn phạm vi sử dụng.

## 6. Mẫu gọi Service không làm đơ UI
```java
saveButton.setEnabled(false);
new SwingWorker<Void, Void>() {
    @Override
    protected Void doInBackground() throws Exception {
        productService.save(form); // chạy nền, không đụng Swing ở đây
        return null;
    }
    @Override
    protected void done() {
        try {
            get(); // ném lại lỗi từ doInBackground
            JOptionPane.showMessageDialog(panel, "Đã lưu thành công");
            reloadTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(panel, "Không lưu được dữ liệu");
            // log ex đầy đủ ở logger, không show SQL/password
        } finally {
            saveButton.setEnabled(true);
        }
    }
}.execute();
```

## 7. Checklist khởi động từ mẫu
- [ ] `Main.java` hiển thị được Swing.
- [ ] Maven resolve được MySQL driver.
- [ ] MySQL tạo được schema trong `03_THIET_KE_DATABASE_MYSQL.md`.
- [ ] Biến môi trường cấu hình hợp lệ, `ConnectionFactory` connect.
- [ ] `JdbcProductDAO.findNames` đọc product vừa chèn.
- [ ] Sau đó bắt đầu **giai đoạn 03** thay các màn hình placeholder bằng form thật.

**Giới hạn:** snippet đơn lẻ không cấu thành ứng dụng bán điện thoại; phải viết toàn bộ những module trong các giai đoạn. Không dùng ví dụ trên như bằng chứng project đã hoàn tất.
