# GIAI ĐOẠN 02 — MYSQL, JDBC, SCHEMA, MODEL, DAO

**Đích:** các model có lưu trữ thật; truy vấn MySQL bằng JDBC, đủ khóa ngoại và transaction.  
**Nguồn:** bài #34–36, #54–61, #72–82, #124.  
**Phụ thuộc:** 01 hoàn thành.

## 1. Các bước thực hiện
- [ ] **01.** Cài MySQL Server + Workbench; đặt database `storephone_db` (`utf8mb4`).
- [ ] **02.** Chạy toàn bộ script trong `03_THIET_KE_DATABASE_MYSQL.md`; kiểm tra 7 bảng `roles`, `users`, `products`, `carts`, `cart_items`, `orders`, `order_items`.
- [ ] **03.** Thêm Maven dependency `com.mysql:mysql-connector-j` theo phiên bản tương thích; không thêm Spring Data/JPA/Hibernate.
- [ ] **04.** Viết `DatabaseConfig` đọc file cấu hình ngoài Git: `db.url`, `db.username`, `db.password`; tạo mẫu `config.example.properties` không chứa mật khẩu thật.
- [ ] **05.** Viết `ConnectionFactory.getConnection()`, mở kết nối khi cần. DAO dùng `try-with-resources` cho `Connection/PreparedStatement/ResultSet`.
- [ ] **06.** Tạo model Java với kiểu phù hợp: `Long`, `String`, `BigDecimal`, `int`, `boolean`, `LocalDateTime`; không gắn annotation `@Entity`/`@ManyToOne`.
- [ ] **07.** Tạo interface `RoleDAO`, `UserDAO`, `ProductDAO`, `CartDAO`, `OrderDAO`; sau đó tạo `Jdbc*DAO` (triển khai SQL).
- [ ] **08.** Tạo lớp mapper riêng (hoặc phương thức private) để chuyển `ResultSet` → Model; SQL không được kết hợp chuỗi từ input người dùng.
- [ ] **09.** Viết dữ liệu seed: 2 role và tối thiểu 1 admin hash BCrypt (có thể dùng `SeedAdmin` riêng). Nhập 12–20 điện thoại test bằng UI đợt 05 hoặc seed SQL hợp lệ.
- [ ] **10.** Viết test CRUD repository, FK, SQL injection qua keyword, rollback chủ động; log lỗi.
- [ ] **11.** Commit `feat: mysql schema and jdbc dao`.

## 2. Luồng A — Kết nối DB
`Main` → `DatabaseConfig` → `ConnectionFactory.getConnection()` → MySQL; nếu thất bại: hiển thị màn hình báo “Không kết nối được CSDL”, hướng dẫn kiểm tra MySQL/config; **không giả bộ load dữ liệu thành công**.

## 3. Luồng B — Query sản phẩm mẫu
`ProductService.listAll` → `ProductDAO.findAll()` → `SELECT ... FROM products` → `ResultSet` → `Product` → trả `List<Product>` → test console/JUnit. Đến giai đoạn 05 mới nối vào JTable. Phân biệt lỗi kết nối và danh sách hợp lệ rỗng.

## 4. Luồng C — Giao dịch đồng bộ
Bắt đầu từ service `connection.setAutoCommit(false)` → gọi nhiều DAO với **cùng Connection** → `commit`; gặp lỗi → `rollback`. DAO `insertOrder` **không được tự commit** khi service đang quản lý transaction.

## 5. API DAO tối thiểu
| DAO | Phương thức cần viết theo giai đoạn |
|---|---|
| `UserDAO` | `findByEmail`, `findById`, `insert`, `update`, `deactivate`, `search(page)` |
| `RoleDAO` | `findByName`, `findAll` |
| `ProductDAO` | `insert`, `update`, `findById`, `search(criteria)`, `count(criteria)`, `deactivate`, `decreaseStock` |
| `CartDAO` | `findOrCreateByUser`, `findItems`, `upsertItem`, `deleteItem`, `recomputeTotal`, `clear` |
| `OrderDAO` | `insertOrder`, `insertItem`, `findById`, `findByUserId`, `findAll`, `updateStatus`, `findItemsByOrderId` |

**Không cần implement tất cả method ngay:** tạo interface rồi triển khai theo từng feature; tránh viết quá nhiều code chưa có test.

## 6. Kiểm thử quan trọng
- [ ] Kết nối đúng → nhận 2 roles; MySQL tắt → thông báo đúng.
- [ ] Insert user email trùng → báo trùng; không phá app.
- [ ] PreparedStatement ngăn input tìm kiếm `"' OR 1=1 --"` biến thành SQL.
- [ ] `products.quantity` âm / `cart_items.quantity=0` bị từ chối.
- [ ] FK chặn `order_items` tham chiếu order không tồn tại.
- [ ] Bắt buộc dùng `BigDecimal` cho số tiền và tính tổng.
- [ ] `rollback` sau lỗi tạo order khiến **không có order mồ côi**.

## 7. Điều kiện nghiệm thu
Chạy một chương trình/JUnit test: connect → insert sản phẩm mẫu → select lại → update → select → deactivate; quan sát đúng dữ liệu qua Workbench, code không cần chạy Spring. Có ảnh ERD, ảnh kết nối thành công.
