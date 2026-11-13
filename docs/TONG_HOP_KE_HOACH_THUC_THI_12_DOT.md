# TỔNG HỢP KẾ HOẠCH & NỘI DUNG THỰC THI TOÀN BỘ 12 ĐỢT — STOREPHONE SWING DESKTOP

> **Dự án**: `com.storephone` (`storephone-swing`) — Phần mềm Quản lý & Bán Điện Thoại chạy Desktop  
> **Công nghệ**: Pure Java 17 + Swing MVC (`JFrame`, `CardLayout`, `JPanel`, `JDialog`, `JTable`, `SwingWorker`) + MySQL 8+ (JDBC `PreparedStatement`) + BCrypt + JUnit 5  
> **Chiến lược thi công**: Không sử dụng Spring Boot / Hibernate / ORM; thực thi tuần tự theo **từng lát cắt dọc (Vertical Slice)** từ Đợt 01 đến Đợt 12, kiểm thử trực tiếp trên MySQL thật ở mỗi đợt.

---

## PHẦN I. KIẾN TRÚC TỔNG THỂ & BẤT BIẾN KỸ THUẬT

### 1. Phân lớp Kiến trúc 5 Tầng
1. **`view` (`com.storephone.view`)**: Dựng giao diện Swing, hiển thị dữ liệu từ DTO/Model, gửi thao tác sang Controller/Service qua `UiTaskRunner` (`SwingWorker`) và cập nhật lại UI trên EDT. Không chứa SQL hay `Connection`.
2. **`controller` (`com.storephone.controller`)**: Điều phối chuyển màn hình (`NavigationController`, `NavigationRoutes`), gác cổng quyền lớp UI, đóng gói dữ liệu form thành Request DTO và điều hướng theo vai trò.
3. **`service` (`com.storephone.service`)**: Chứa toàn bộ nghiệp vụ, kiểm tra đầu vào (`InputValidator`), phân quyền lớp nghiệp vụ (`AuthorizationService`), băm/xác thực mật khẩu (`PasswordHasher`), quản lý tệp ảnh (`ImageStorage`) và điều phối giao dịch JDBC (`setAutoCommit(false)` $\rightarrow$ `commit()` / `rollback()`).
4. **`dao` & `dao.jdbc` (`com.storephone.dao.*`)**: Truy cập MySQL thuần `PreparedStatement`, ánh xạ `ResultSet` tập trung tại `JdbcRowMapper`, hỗ trợ truyền chung `Connection` cho các thao tác nguyên tử.
5. **`security`, `config`, `util`, `model`, `dto`, `exception`**: Quản lý phiên trong RAM (`AppSession`), cấu hình ngoài mã nguồn (`DatabaseConfig`, `ConnectionFactory`), định dạng tiền tệ `BigDecimal` (`MoneyFormatter`) và ngoại lệ nghiệp vụ tiếng Việt rõ ràng.

### 2. Bảy Bất Biến Toàn Hệ Thống
- **Tiền tệ**: `BigDecimal` ở Java và `DECIMAL(15,0)` ở MySQL; tuyệt đối không dùng `float`/`double`.
- **Mật khẩu**: Băm BCrypt (`work factor = 12`); nhận từ `JPasswordField.getPassword()` dạng `char[]` và xóa trắng mảng (`Arrays.fill`) ngay sau khi xử lý; không log hay in `passwordHash`.
- **Phân quyền 2 lớp (`GUEST` / `USER` / `ADMIN`)**:
  - Lớp 1 (UI): Ẩn vùng Quản trị với `GUEST`/`USER`; chặn route trái phép tại `NavigationController`.
  - Lớp 2 (Service): Gọi `requireAdmin()` cho nghiệp vụ quản trị, `requireAuthenticated()` cho nghiệp vụ cá nhân và kiểm tra quyền sở hữu `order.getUserId().equals(currentUser.userId())`.
- **Chống đơ giao diện (EDT Safety)**: Mọi truy vấn DB/đọc ghi ảnh từ UI đều chạy nền bằng `UiTaskRunner.runAsync(...)`, vô hiệu hóa nút bấm trong lúc xử lý để chống bấm đúp.
- **Giao dịch Nguyên tử (ACID)**: Đặt hàng (`CheckoutService`) và Hủy đơn hoàn kho (`OrderService`) dùng duy nhất một `Connection` với khóa dòng `SELECT ... FOR UPDATE`.
- **Lưu trữ Ảnh**: Sao chép vào `data/images/products/` hoặc `data/images/avatars/` với tên `<UUID>.<ext>`, trong DB chỉ lưu đường dẫn tương đối (`products/<uuid>.png`, `avatars/<uuid>.jpg`).
- **Xóa mềm An toàn**: Khóa tài khoản hoặc ngừng bán sản phẩm bằng `active = FALSE` khi đã có dữ liệu liên kết, bảo toàn lịch sử đơn hàng.

---

## PHẦN II. 7 TINH CHỈNH KỸ THUẬT TRỌNG TÂM

1. **Gia cố `ImageStorage` (`util/ImageStorage.java`)**: Kiểm tra đuôi (`png`, `jpg`, `jpeg`, `webp`), kích thước `<= 5MB`, xác thực nội dung ảnh thật bằng `ImageIO.read(...) != null` (chặn tệp `.txt` đổi đuôi), trả về đường dẫn tương đối `subFolder/<uuid>.ext`, phân giải từ `rootDir` và bổ sung `deleteQuietly(relativePath)` khi lưu DB thất bại.
2. **Huy hiệu Giỏ hàng trên Header (`AppSession` + `MainFrame`)**: Lưu `cartQuantity` trong `AppSession`, hiển thị nút `Giỏ hàng (N)` trên Header khi đã đăng nhập; đồng bộ với `COALESCE(SUM(quantity), 0)` khi đăng nhập/thêm/sửa/xóa/đặt hàng và đưa về `0` trên RAM khi đăng xuất mà không xóa dữ liệu giỏ trong MySQL.
3. **Bảo vệ Admin Cuối cùng (`UserDAO` / `JdbcUserDAO` / `UserService`)**: Bổ sung `countActiveAdmins()` để chặn tự khóa/tự hạ quyền chính mình và chặn khóa hoặc hạ quyền `ADMIN` hoạt động cuối cùng.
4. **Bộ lọc Đa khoảng giá & Sắp xếp Ổn định (`ProductFilterCriteria` / `JdbcProductDAO`)**: Hỗ trợ nhiều khoảng giá kết hợp `OR` trong nhóm giá và `AND` giữa các nhóm (`keyword`, `factory`, `target`, `priceRanges`), dùng chung mệnh đề `WHERE` cho `COUNT(*)` và `SELECT`, kèm khóa phụ `id DESC`.
5. **SQL Aggregate Dashboard (`DashboardDAO` / `JdbcDashboardDAO`)**: Truy vấn trực tiếp trên MySQL các chỉ số KPI, doanh thu đơn `COMPLETED`, 5 đơn gần nhất và Top 5 điện thoại bán chạy.
6. **Executable Fat JAR (`pom.xml`)**: Bổ sung `maven-shade-plugin` ở Đợt 12 đóng gói kèm `mysql-connector-j` và `jbcrypt` với `Main-Class: com.storephone.Main`.
7. **Vệ sinh Git & Giấy phép**: Giữ nguyên `LICENSE.MD` và `LICENSE_VI.MD` theo lưu ý tại `00_BAT_DAU_O_DAY.md`, đảm bảo `.gitignore` loại trừ `config.properties`, `data/`, `target/`.

---

## PHẦN III. NỘI DUNG CHI TIẾT THỰC THI TỪNG ĐỢT (ĐỢT 01 → ĐỢT 12)

### Đợt 01 — Khởi tạo Maven Java 17, Khung Giao diện Swing & Điều hướng (`GIAI_DOAN_01_KHOI_TAO.md`)
- **Trạng thái**: Đã hoàn thành nền tảng.
- **Thành phần chính**:
  - `pom.xml` (Java 17, loại bỏ hoàn toàn Spring Boot, tích hợp `mysql-connector-j`, `jbcrypt`, `junit-jupiter`).
  - `Main.java`, `config/AppContext.java`, `controller/NavigationRoutes.java`, `controller/NavigationController.java`.
  - `view/MainFrame.java` (1 `JFrame` duy nhất + `CardLayout`), `view/UiTheme.java`, `view/HomeOverviewPanel.java`.
- **Nghiệm thu (`P01-01`–`P01-04`)**: Mở ứng dụng trên EDT (`SwingUtilities.invokeLater`), chuyển các thẻ màn hình mượt, gác cổng quyền cơ bản theo trạng thái `AppSession`.

### Đợt 02 — Thiết kế CSDL MySQL 7 Bảng & Tầng JDBC DAO (`GIAI_DOAN_02_CSDL_DAO.md`)
- **Trạng thái**: Đã hoàn thành nền tảng.
- **Thành phần chính**:
  - `sql/01_schema.sql` (7 bảng `roles`, `users`, `products`, `carts`, `cart_items`, `orders`, `order_items` với đầy đủ `PK`, `FK`, `UNIQUE`, `CHECK`), `sql/02_seed_sample_products.sql`.
  - `config/DatabaseConfig.java`, `config/ConnectionFactory.java`, `util/DatabaseInitializer.java`, `util/SeedAdmin.java`.
  - Toàn bộ Model (`Role`, `User`, `Product`, `Cart`, `CartItem`, `Order`, `OrderItem`, `OrderStatus`) và DAO (`JdbcRoleDAO`, `JdbcUserDAO`, `JdbcProductDAO`, `JdbcCartDAO`, `JdbcOrderDAO`, `JdbcRowMapper`).
- **Nghiệm thu (`P02-01`–`P02-04`)**: Kiểm thử tích hợp `JdbcDaoIntegrationTest` và `CoreFoundationUnitTest` xanh 100% trên MySQL thật.

### Đợt 03 — Đăng ký, Đăng nhập, Đăng xuất & Phân quyền 2 Lớp (`GIAI_DOAN_03_AUTH_PHAN_QUYEN.md`)
- **Mục tiêu**: Xác thực người dùng bằng BCrypt, quản lý phiên `AppSession` kèm huy hiệu giỏ hàng, phân luồng `ADMIN` $\rightarrow$ `ADMIN_DASHBOARD` và `USER` $\rightarrow$ `HOME`.
- **Tệp tạo mới / cập nhật**:
  - `dto/LoginRequest.java`, `dto/RegisterRequest.java` (nhận `char[]` mật khẩu và có hàm xóa trắng `clearSensitive()`).
  - `service/AuthService.java`: chuẩn hóa email `.trim().toLowerCase()`, kiểm tra cú pháp email & mật khẩu $\ge 8$ ký tự, đối chiếu xác nhận mật khẩu, gán cố định vai trò `USER` khi đăng ký công khai, bắt lỗi trùng email (`DuplicateEmailException` + mã SQL `1062`), chặn đăng nhập khi `active = false`, đồng bộ `cartQuantity` từ `CartDAO` khi đăng nhập thành công.
  - `security/AppSession.java` (thêm `cartQuantity`), `controller/AuthController.java`.
  - `view/LoginPanel.java`, `view/RegisterPanel.java` (chạy qua `UiTaskRunner`, xóa `JPasswordField` sau khi xử lý).
  - Đấu nối `AppContext` & `MainFrame` (thay placeholder `LOGIN`/`REGISTER`, gắn nút `Giỏ hàng (N)` trên Header).
  - Kiểm thử: `src/test/java/com/storephone/AuthServiceTest.java` (`TC-01`–`TC-04`, khóa tài khoản, đăng xuất).

### Đợt 04 — Quản lý Người dùng (Admin CRUD), Ảnh Đại diện & Hồ sơ Cá nhân (`GIAI_DOAN_04_QUAN_LY_USER.md`)
- **Mục tiêu**: Cho phép `ADMIN` tìm kiếm/phân trang, tạo mới theo vai trò, xem chi tiết (ẩn hash mật khẩu), sửa thông tin/vai trò, khóa/mở khóa an toàn; cho phép người dùng tự cập nhật hồ sơ & avatar.
- **Tệp tạo mới / cập nhật**:
  - Gia cố `util/ImageStorage.java` (`ImageIO.read` kiểm tra ảnh thật, lưu đường dẫn tương đối `avatars/<uuid>.ext`, `deleteQuietly`).
  - Bổ sung `countActiveAdmins()` vào `dao/UserDAO.java` & `dao/jdbc/JdbcUserDAO.java`.
  - `dto/UserCreateRequest.java`, `dto/UserAdminUpdateRequest.java`, `dto/ProfileUpdateRequest.java`.
  - `service/UserService.java`: bắt buộc `requireAdmin()` cho thao tác quản trị, `requireAuthenticated()` cho cập nhật hồ sơ cá nhân; không ghi đè `password_hash` khi sửa thông tin thường; chặn tự khóa/tự hạ quyền chính mình và chặn khóa/hạ quyền `ADMIN` hoạt động cuối cùng; đồng bộ lại tên hiển thị trong `AppSession` nếu người dùng tự sửa hồ sơ.
  - `view/table/UserTableModel.java`, `view/AdminUserPanel.java`, `view/UserFormDialog.java` (`JFileChooser` chọn & xem trước avatar), `view/ProfilePanel.java`.
  - Đấu nối vào `AppContext` & `MainFrame` (`ADMIN_USERS`, `PROFILE`).
  - Kiểm thử: `src/test/java/com/storephone/UserServiceTest.java` (`P04-01`–`P04-06`).

### Đợt 05 — Quản lý Sản phẩm Điện thoại & Tải Ảnh Đại diện (`GIAI_DOAN_05_SAN_PHAM_ANH.md`)
- **Mục tiêu**: `ADMIN` thêm/sửa/xem chi tiết/ngừng bán (`active = FALSE`) hoặc xóa cứng nếu sản phẩm chưa từng nằm trong giỏ/đơn hàng, tải ảnh điện thoại có xem trước.
- **Tệp tạo mới / cập nhật**:
  - `dto/ProductSaveRequest.java`.
  - `service/ProductService.java`: kiểm tra `requireAdmin()` cho mọi thao tác ghi (`TC-04`), kiểm tra `name`, `factory`, `target`, `shortDesc` không rỗng, `price >= 0` (`BigDecimal`, `TC-05`), `quantity >= 0`; sao chép ảnh vào `products/<uuid>.ext` và tự động dọn ảnh mới nếu lưu DB lỗi.
  - `view/table/ProductTableModel.java`, `view/AdminProductPanel.java`, `view/ProductFormDialog.java` (`JComboBox` hãng & mục đích sử dụng + cho phép nhập hãng mới, `JFileChooser` preview ảnh).
  - Đấu nối vào `AppContext` & `MainFrame` (`ADMIN_PRODUCTS`).
  - Kiểm thử: `src/test/java/com/storephone/ProductServiceTest.java` (`TC-04`, `TC-05`, `P05-01`–`P05-05`).

### Đợt 06 — Danh mục Công khai & Hộp thoại Chi tiết Điện thoại (`GIAI_DOAN_06_TRANG_CHU_CHI_TIET.md`)
- **Mục tiêu**: `GUEST`, `USER`, `ADMIN` đều xem được danh sách điện thoại đang bán (`active = TRUE`), mở chi tiết đầy đủ thông số, ảnh, tồn kho; khi `GUEST` bấm "Thêm vào giỏ" thì thông báo và chuyển sang `LOGIN`.
- **Tệp tạo mới / cập nhật**:
  - `view/CatalogPanel.java`: lưới thẻ sản phẩm (Card Grid) kèm nút xem chi tiết & thêm nhanh vào giỏ (vô hiệu hóa nút mua khi `quantity == 0` với nhãn "Hết hàng").
  - `view/ProductDetailDialog.java`: hiển thị ảnh lớn, hãng, đối tượng sử dụng, giá VND (`MoneyFormatter`), tồn kho, mô tả ngắn & mô tả chi tiết, `JSpinner` chọn số lượng mua.
  - Đấu nối vào `MainFrame` (`CATALOG`) và tự động làm mới dữ liệu khi chuyển tới màn hình.
- **Nghiệm thu (`P06-01`–`P06-05`, `TC-06`)**: Không lộ sản phẩm `active = FALSE` ở catalog công khai; `GUEST` xem danh mục/chi tiết không cần đăng nhập.

### Đợt 07 — Tìm kiếm, Bộ lọc Đa tiêu chí, Sắp xếp & Phân trang (`GIAI_DOAN_07_TIM_KIEM_LOC_SAP_XEP_PHAN_TRANG.md`)
- **Mục tiêu**: Lọc kết hợp từ khóa + nhiều hãng (`IN`) + nhiều nhu cầu (`IN`) + nhiều khoảng giá (`OR` trong nhóm giá, `AND` giữa các nhóm), sắp xếp theo whitelist an toàn, phân trang chuẩn và giữ nguyên bộ lọc sau khi đóng chi tiết máy.
- **Tệp tạo mới / cập nhật**:
  - Cập nhật `dto/ProductFilterCriteria.java`: bổ sung `PriceRange(BigDecimal min, BigDecimal max)` và `List<PriceRange> priceRanges` (Dưới 10 triệu, 10–15 triệu, 15–25 triệu, Trên 25 triệu).
  - Cập nhật `dao/jdbc/JdbcProductDAO.java`: xây dựng `WHERE` động tham số hóa 100%, dùng chung `WHERE` cho `COUNT(*)` và `SELECT ... LIMIT ? OFFSET ?`, khóa sắp xếp phụ ổn định `id DESC`.
  - Mở rộng `view/CatalogPanel.java`: ô tìm kiếm từ khóa, nhóm `JCheckBox` hãng, nhu cầu, khoảng giá, `JComboBox` sắp xếp, thanh phân trang `Đầu / Trước / Trang x/y / Sau / Cuối`, nút `Xóa bộ lọc`, Empty State khi `total = 0`.
  - Kiểm thử: `src/test/java/com/storephone/ProductCatalogFilterTest.java` (`TC-07`–`TC-10`, `P07-01`–`P07-06`).

### Đợt 08 — Giỏ hàng Cá nhân Bền vững trên MySQL (`GIAI_DOAN_08_GIO_HANG.md`)
- **Mục tiêu**: Mỗi `USER` có giỏ riêng trong MySQL (`carts` + `cart_items`), cộng dồn số lượng khi thêm trùng máy, kiểm tra `1 <= qty <= product.quantity`, tính tổng tiền bằng `BigDecimal`, giữ nguyên giỏ sau khi đăng xuất / đăng nhập lại.
- **Tệp tạo mới / cập nhật**:
  - `dto/CartSummary.java` (danh sách dòng giỏ + tổng số lượng + tổng tiền `BigDecimal`).
  - `service/CartService.java`: bắt buộc `requireAuthenticated()`, lấy `userId` từ `AppSession`; kiểm tra `product.isActive()` và `existingQty + addedQty <= product.getQuantity()`; đồng bộ `total_quantity` trong DB và `appSession.setCartQuantity(...)`.
  - `view/table/CartItemTableModel.java`, `view/CartPanel.java`: nút `+1`, `-1`, cập nhật số lượng, xóa dòng, tổng thành tiền `BigDecimal`, nút "Tiến hành Đặt hàng" sang `CHECKOUT`.
  - Kiểm thử: `src/test/java/com/storephone/CartServiceTest.java` (`TC-11`–`TC-13`, `P08-01`–`P08-06`).

### Đợt 09 — Thanh toán / Đặt hàng Nguyên tử trong 1 Transaction (`GIAI_DOAN_09_CHECKOUT_DAT_HANG.md`)
- **Mục tiêu**: Chốt đơn từ giỏ hàng trong **một JDBC Transaction duy nhất** với `SELECT ... FOR UPDATE`, chụp snapshot tên & đơn giá vào `order_items`, trừ tồn kho và xóa rỗng giỏ hàng; nếu lỗi giữa chừng thì `rollback()` toàn bộ.
- **Tệp tạo mới / cập nhật**:
  - `dto/CheckoutRequest.java` (`receiverName`, `receiverPhone`, `receiverAddress`).
  - `service/CheckoutService.java`: kiểm tra `requireAuthenticated()`, validate họ tên/SĐT/địa chỉ (`TC-15`); mở `Connection` (`autoCommit = false`), chặn giỏ rỗng (`TC-14`), sắp xếp dòng giỏ theo `productId ASC` tránh deadlock, gọi `findByIdForUpdate` kiểm tra `active` & `quantity >= requested` (`TC-17`), gọi `deductStock` (`TC-16`), ghi `orders` (`PENDING`) + `order_items` (snapshot giá), `clearCart`, `commit()`, cập nhật `appSession.setCartQuantity(0)`. Hỗ trợ hook kiểm thử lỗi giữa chừng để xác minh `rollback()` không để lại đơn mồ côi (`TC-18`).
  - `view/CheckoutPanel.java`: tự điền gợi ý thông tin từ hồ sơ `User`, bảng xem trước đơn, khóa nút xác nhận khi `SwingWorker` đang chạy chống bấm đúp (`P09-06`).
  - Kiểm thử: `src/test/java/com/storephone/CheckoutServiceTest.java` (`TC-14`–`TC-18`, `P09-01`–`P09-06`).

### Đợt 10 — Quản lý Đơn hàng (Admin State Machine) & Lịch sử Mua hàng (`GIAI_DOAN_10_DON_HANG_LICH_SU.md`)
- **Mục tiêu**: `ADMIN` tra cứu/lọc đơn theo trạng thái, duyệt chuyển trạng thái hợp lệ và hoàn kho **đúng 1 lần** khi hủy đơn; `USER` chỉ xem được lịch sử & chi tiết đơn của chính mình với giá snapshot bất biến.
- **Tệp tạo mới / cập nhật**:
  - `dto/OrderDetailView.java` (gói `Order` + `List<OrderItem>`).
  - `service/OrderService.java`:
    - `getMyOrders()` & `getOrderDetail(orderId)`: chặn `USER B` xem đơn của `USER A` bằng `AuthorizationException` (`TC-19`).
    - `updateOrderStatus(orderId, nextStatus)` (`ADMIN` only): mở transaction `setAutoCommit(false)`, khóa đơn bằng `findByIdForUpdate`, kiểm tra `current.canTransitionTo(nextStatus)` (`PENDING -> CONFIRMED/CANCELLED`, `CONFIRMED -> SHIPPING/CANCELLED`, `SHIPPING -> COMPLETED`). Khi `nextStatus == CANCELLED`, hoàn kho từng dòng `order_items` qua `restoreStock` rồi `updateStatus` và `commit()` (`TC-20`, `TC-21`).
  - `view/table/OrderTableModel.java`, `view/OrderDetailDialog.java`, `view/AdminOrderPanel.java`, `view/OrderHistoryPanel.java`.
  - Kiểm thử: `src/test/java/com/storephone/OrderServiceTest.java` (`TC-19`–`TC-22`, `P10-01`–`P10-05`).

### Đợt 11 — Bảng Điều khiển Thống kê Admin (`GIAI_DOAN_11_DASHBOARD.md`)
- **Mục tiêu**: Hiển thị số liệu thống kê thực từ MySQL chỉ dành cho `ADMIN`, doanh thu chỉ tính đơn `COMPLETED` (`BigDecimal`), có bảng 5 đơn mới nhất, Top 5 điện thoại bán chạy và nút Làm mới.
- **Tệp tạo mới / cập nhật**:
  - `dto/DashboardMetrics.java` (bao gồm `RecentOrderRow`, `TopProductRow`).
  - `dao/DashboardDAO.java`, `dao/jdbc/JdbcDashboardDAO.java` (truy vấn SQL aggregate trực tiếp trên MySQL).
  - `service/DashboardService.java` (`requireAdmin()`).
  - `view/AdminDashboardPanel.java` (4 thẻ KPI + 2 bảng thống kê + nút Làm mới chạy nền qua `UiTaskRunner`).
  - Kiểm thử: `src/test/java/com/storephone/DashboardServiceTest.java` (`TC-23`, `P11-01`–`P11-05`).

### Đợt 12 — Kiểm thử Tích hợp Đầu-Cuối, Đóng gói Fat JAR & Nghiệm thu (`GIAI_DOAN_12_TEST_BAO_CAO.md`)
- **Mục tiêu**: Hoàn thiện bộ test đầu-cuối `GUEST -> USER -> ADMIN -> USER`, cấu hình `maven-shade-plugin` tạo executable Fat JAR, cập nhật `README.MD` & tích kết quả nghiệm thu vào `docs/05_CHECKLIST_TONG_HOP.md`.
- **Tệp tạo mới / cập nhật**:
  - `src/test/java/com/storephone/EndToEndAcceptanceTest.java` (`TC-01` $\rightarrow$ `TC-24`, bao gồm `TC-24` khởi động lại phiên RAM trong khi dữ liệu MySQL bền vững).
  - Cập nhật `pom.xml` (`maven-shade-plugin`), khôi phục `LICENSE.MD` / `LICENSE_VI.MD`, cập nhật `README.MD` và `docs/05_CHECKLIST_TONG_HOP.md`.

---

## PHẦN IV. MA TRẬN 24 CA KIỂM THỬ NGHIỆM THU (`TC-01` → `TC-24`)

| Mã TC | Kịch bản kiểm thử | Kết quả kỳ vọng | Lớp Test xác minh |
|---|---|---|---|
| `TC-01` | Đăng ký `user@gmail.com` hợp lệ | Tạo user mới với role `USER`, mật khẩu băm BCrypt | `AuthServiceTest` |
| `TC-02` | Đăng ký email trùng lặp | Ném `DuplicateEmailException`, DB không sinh bản ghi trùng | `AuthServiceTest` |
| `TC-03` | Đăng nhập sai mật khẩu / tài khoản khóa | Báo lỗi xác thực, `AppSession` vẫn là `GUEST` | `AuthServiceTest` |
| `TC-04` | `USER` gọi `ProductService.createProduct` | Ném `AuthorizationException` | `ProductServiceTest` |
| `TC-05` | `ADMIN` thêm sản phẩm giá `-1` | Ném `ValidationException`, không ghi DB | `ProductServiceTest` |
| `TC-06` | `GUEST` mở danh mục sản phẩm | Trả về danh sách máy `active = TRUE`, không đòi đăng nhập | `ProductCatalogFilterTest` |
| `TC-07` | Tìm từ khóa không tồn tại | Trả về danh sách rỗng, `totalElements = 0` | `ProductCatalogFilterTest` |
| `TC-08` | Lọc 2 hãng + đa khoảng giá | Thỏa `AND` giữa các nhóm và `OR` giữa các khoảng giá | `ProductCatalogFilterTest` |
| `TC-09` | Sắp xếp giá giảm dần (`PRICE_DESC`) | Giá giảm dần, ổn định theo `id DESC` | `ProductCatalogFilterTest` |
| `TC-10` | `pageSize = 10` với 11 sản phẩm | Chia đúng 2 trang: trang 1 (10 máy), trang 2 (1 máy) | `ProductCatalogFilterTest` |
| `TC-11` | `USER` thêm sản phẩm A 2 lần | Cùng 1 dòng `cart_items` tăng `quantity = 2` | `CartServiceTest` |
| `TC-12` | Thêm B (`qty = 4`) rồi xóa B | `carts.total_quantity` và badge giảm đúng 4 | `CartServiceTest` |
| `TC-13` | `USER` đăng xuất rồi đăng nhập lại | Giỏ hàng & huy hiệu khôi phục nguyên vẹn từ MySQL | `CartServiceTest` |
| `TC-14` | Đặt hàng khi giỏ rỗng | Ném `ValidationException`, không sinh đơn | `CheckoutServiceTest` |
| `TC-15` | Đặt hàng thiếu SĐT / SĐT sai định dạng | Ném `ValidationException`, không sinh đơn | `CheckoutServiceTest` |
| `TC-16` | Đặt sản phẩm A `qty = 2`, kho `= 3` | Tạo đơn `PENDING`, kho còn `1`, giỏ về `0` | `CheckoutServiceTest` |
| `TC-17` | Đặt sản phẩm A `qty = 2`, kho `= 1` | Ném `InsufficientStockException`, rollback nguyên trạng | `CheckoutServiceTest` |
| `TC-18` | Giả lập lỗi SQL sau khi `insertOrder` | `rollback()` toàn bộ, không còn đơn mồ côi | `CheckoutServiceTest` |
| `TC-19` | `USER B` truy cập chi tiết đơn của `USER A` | Ném `AuthorizationException` | `OrderServiceTest` |
| `TC-20` | `ADMIN` hủy đơn `PENDING`/`CONFIRMED` | Chuyển `CANCELLED` và hoàn kho chính xác | `OrderServiceTest` |
| `TC-21` | `ADMIN` bấm hủy lại đơn đã `CANCELLED` | Chặn chuyển trạng thái, không hoàn kho lần 2 | `OrderServiceTest` |
| `TC-22` | Đổi `Product.price` sau khi đã đặt đơn | `order_items.unit_price` giữ nguyên giá snapshot lúc mua | `OrderServiceTest` |
| `TC-23` | Chuyển đơn sang `COMPLETED` | Doanh thu trên `DashboardService` tăng đúng số tiền đơn | `DashboardServiceTest` |
| `TC-24` | Khởi tạo `AppContext` mới (mô phỏng restart) | Dữ liệu MySQL còn nguyên, `AppSession` mới ở trạng thái đăng xuất | `EndToEndAcceptanceTest` |




