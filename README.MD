# StorePhone — Phần Mềm Quản Lý & Bán Điện Thoại (Java Swing + MySQL)

Dự án cuối kỳ môn **IE303** (`IE303_java_storephone`): ứng dụng **Desktop thuần Java 17 Swing + JDBC MySQL 8+**, triển khai theo kiến trúc phân lớp **Model – View – Controller – Service – DAO** (hoàn toàn **không sử dụng Spring Boot / Web Server / JPA**).

---

## 1. Công nghệ & Quyết định kỹ thuật
| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | **Java 17** |
| Giao diện Desktop | **Java Swing** (`JFrame`, `JPanel`, `JTable`, `JDialog`, `CardLayout`, `SwingWorker`) |
| Quản lý build | **Apache Maven** (`./mvnw`, không dùng Spring Boot) |
| Cơ sở dữ liệu | **MySQL 8+** (`utf8mb4`), truy xuất qua **JDBC + `PreparedStatement`** |
| Tiền tệ | `BigDecimal` (Java) ↔ `DECIMAL(15,0)` (MySQL) |
| Bảo mật mật khẩu | **BCrypt** (`org.mindrot:jbcrypt`), không lưu mật khẩu plaintext |
| Phiên đăng nhập | `AppSession` lưu trong RAM tiến trình Desktop (`GUEST` / `USER` / `ADMIN`) |
| Kiểm thử | **JUnit 5** |

---

## 2. Cấu trúc thư mục dự án
```text
.
├── pom.xml                        # Cấu hình Maven Java 17 thuần (MySQL JDBC, jBCrypt, JUnit 5)
├── config.example.properties      # Mẫu cấu hình kết nối MySQL (copy thành config.properties)
├── sql/
│   ├── 01_schema.sql              # Khởi tạo 7 bảng CSDL chuẩn + seed 2 role ADMIN/USER
│   └── 02_seed_sample_products.sql# Dữ liệu điện thoại mẫu phục vụ kiểm thử
├── docs/                          # Trọn bộ 20 tài liệu thiết kế & lộ trình 12 giai đoạn
└── src/
    ├── main/
    │   ├── java/com/storephone/
    │   │   ├── Main.java          # Điểm khởi chạy ứng dụng Desktop (Swing EDT)
    │   │   ├── config/            # AppContext, DatabaseConfig, ConnectionFactory
    │   │   ├── model/             # Role, User, Product, Cart, CartItem, Order, OrderItem, OrderStatus
    │   │   ├── dto/               # UserSession, PageResult, Request/Form/Criteria DTOs
    │   │   ├── dao/               # RoleDAO, UserDAO, ProductDAO, CartDAO, OrderDAO
    │   │   ├── dao/jdbc/          # Triển khai JDBC PreparedStatement + hỗ trợ Transaction
    │   │   ├── service/           # AuthorizationService, HealthCheckService (+ các Service theo đợt)
    │   │   ├── controller/        # NavigationRoutes, NavigationController
    │   │   ├── view/              # MainFrame + hệ thống Panel màn hình theo CardLayout
    │   │   ├── security/          # AppSession, PasswordHasher (BCrypt)
    │   │   ├── util/              # InputValidator, MoneyFormatter, ImageStorage, UiTaskRunner, SeedAdmin
    │   │   └── exception/         # ValidationException, AuthorizationException, DuplicateEmailException...
    │   └── resources/
    │       └── app.properties     # Thông tin hiển thị mặc định của ứng dụng
    └── test/java/com/storephone/  # Unit test & MySQL JDBC Integration test (JUnit 5)
```

---

## 3. Hướng dẫn cài đặt & Chạy ứng dụng

### Bước 1: Chuẩn bị Cơ sở dữ liệu MySQL
1. Mở **MySQL Workbench** (MySQL 8.0+) và chạy tệp `sql/01_schema.sql` để tạo CSDL cùng 7 bảng (`roles`, `users`, `products`, `carts`, `cart_items`, `orders`, `order_items`) và 2 vai trò `ADMIN`, `USER`.
2. (Tùy chọn) Chạy `sql/02_seed_sample_products.sql` để thêm danh sách điện thoại mẫu.
   - *Hoặc sau khi đã cấu hình `config.properties` ở Bước 2, bạn có thể chạy thẳng bằng lệnh JDBC:*
     ```bash
     ./mvnw compile exec:java -Dexec.mainClass="com.storephone.util.DatabaseInitializer" -Dexec.args="--with-sample-products"
     ```

### Bước 2: Cấu hình kết nối local
Sao chép `config.example.properties` thành `config.properties` (tệp này đã nằm trong `.gitignore`, tuyệt đối không commit mật khẩu lên Git):
```bash
cp config.example.properties config.properties
```
Điền `db.url`, `db.username`, `db.password` phù hợp với MySQL trên máy của bạn.

### Bước 3: Tạo tài khoản ADMIN đầu tiên (BCrypt)
```bash
STOREPHONE_ADMIN_EMAIL="admin@storephone.local" \
STOREPHONE_ADMIN_PASSWORD="AdminPassword123!" \
STOREPHONE_ADMIN_NAME="Quản trị StorePhone" \
./mvnw compile exec:java -Dexec.mainClass="com.storephone.util.SeedAdmin"
```

### Bước 4: Kiểm thử, Đóng gói Fat JAR & Khởi chạy ứng dụng Swing
```bash
# Biên dịch và chạy toàn bộ bộ kiểm thử JUnit 5 (TC-01 -> TC-24)
./mvnw clean test

# Đóng gói Executable Fat JAR (gói sẵn MySQL Connector/J + jBCrypt)
./mvnw clean package -DskipTests

# Chạy trực tiếp từ tệp JAR độc lập (cần có config.properties cùng thư mục chạy)
java -jar target/storephone-swing-1.0.0-SNAPSHOT.jar

# Hoặc mở cửa sổ ứng dụng qua Maven
./mvnw exec:java
```

---

## 4. Nguồn gốc Nghiệp vụ, Điểm Mở rộng & Giới hạn Phạm vi
- **Nguồn gốc đối chiếu**: Chuyển đổi nghiệp vụ thương mại điện tử cốt lõi (User/Role, Product, Image, Auth, Cart, Checkout, Order/OrderDetail, History, Dashboard, Pagination, Filter, Sort) sang mô hình **ứng dụng bán điện thoại chạy Desktop thuần Java 17 Swing + JDBC MySQL** hoàn toàn mới, không sao chép mã nguồn khóa học gốc.
- **Các điểm kỹ thuật gia cố nổi bật**:
  - Giao dịch nguyên tử trên một `Connection` duy nhất với khóa dòng `SELECT ... FOR UPDATE` khi Đặt hàng (trừ kho + lưu snapshot giá + xóa giỏ) và khi Hủy đơn (hoàn kho đúng 1 lần).
  - Xác thực nội dung ảnh thật qua `ImageIO.read` (chặn tệp văn bản/thực thi đổi đuôi), lưu đường dẫn tương đối `products/<uuid>.ext` và `avatars/<uuid>.ext`.
  - Chặn quản trị viên tự khóa/tự hạ quyền chính mình và bảo vệ tài khoản `ADMIN` hoạt động cuối cùng.
  - Mọi tác vụ I/O cơ sở dữ liệu và tệp ảnh chạy trên luồng nền `SwingWorker` (`UiTaskRunner`), giữ giao diện Swing luôn mượt.
- **Giới hạn hiện tại & Hướng phát triển**: Chưa tích hợp cổng thanh toán trực tuyến, quản lý IMEI/biến thể màu-dung lượng riêng, in hóa đơn PDF và thông báo thời gian thực qua socket.

---

## 5. Tài liệu Kiến trúc & Lộ trình 12 Đợt
Xem chi tiết trong thư mục [`docs/`](docs/00_BAT_DAU_O_DAY.md):
1. `docs/TONG_HOP_KE_HOACH_THUC_THI_12_DOT.md` — Bản tổng hợp chi tiết toàn bộ 12 đợt thi công & ma trận 24 ca kiểm thử `TC-01`–`TC-24`.
2. `docs/00_BAT_DAU_O_DAY.md` → `docs/07_MAU_CODE_KHOI_DONG.md` — Kiến trúc MVC Swing, ERD MySQL 7 bảng, sơ đồ luồng nghiệp vụ, checklist nghiệm thu.
3. `docs/GIAI_DOAN_01_KHOI_TAO.md` → `docs/GIAI_DOAN_12_TEST_BAO_CAO.md` — Đặc tả từng giai đoạn triển khai.
