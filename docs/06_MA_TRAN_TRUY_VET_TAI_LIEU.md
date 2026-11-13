# MA TRẬN TRUY VẾT: BÀI HỌC GỐC → MODULE SWING

Nguồn: tệp `[PUBLIC] Spring MVC - hoidanit(2).txt` v6.0, kiểm tra 13/11/2026. Các số `#` là **số bài** trong tài liệu chứ không phải trang. Các phần thiếu chi tiết trong tài liệu được đánh dấu `Đề xuất`, tuyệt đối không coi đó là yêu cầu được tác giả mô tả đầy đủ.

| Bài gốc | Điều tài liệu nói | Giai đoạn | Cách chuyển |
|---|---|---|---|
| #6, #10, #21–25 | Java 17, Git, Maven, cấu trúc | 01 | Maven Java desktop; branch/commit |
| #26–32, #43, #45, #59–65 | DI, MVC, Repository, Service | 01–02 | Constructor injection tự viết; Controller/Service/DAO |
| #34–36, #54–58 | MySQL, kết nối và truy xuất | 02 | JDBC/SQL + schema |
| #51–53, #62–70 | Tạo, danh sách, xem, sửa, xóa user | 04 | JDialog + JTable + UserService/DAO |
| #72–76 | Guest/User/Admin, Product, Order, DB | 02–03 | role matrix, ERD |
| #77–82 | 1:N, N:N qua order_detail, 1:1 | 02 | Khóa ngoại MySQL |
| #83–85 | Giao diện admin, header/sidebar/footer | 01, 04–05 | `MainFrame`/sidebar/CardLayout |
| #86–91 | Preview, lưu file ảnh, DB lưu tên | 05 | `JFileChooser` + `ImageStorage` cho avatar và sản phẩm |
| #92–94 | Hash password, CRUD user | 03–04 | BCrypt và UserDAO |
| #95–98 | Trang chủ/client và view detail | 06 | HomePanel và ProductDetailDialog |
| #99–104 | Product fields, validate, CRUD | 05 | Form Swing + ProductService |
| #105–106 | Load product và chi tiết | 06 | Query DAO và render Swing |
| #107–112 | Register, DTO, mapper, custom validation, login UI | 03 | RegisterRequest/AuthService |
| #113–118 | Spring Security/authentication | 03 | BCrypt + AuthorizationService, **không có filter web** |
| #119 | role USER/ADMIN, guest xem public, cart/order yêu cầu login | 03 | Guard ở Service + điều hướng Swing |
| #120 | Logout + ẩn/hiện menu theo login | 03 | `AppSession.clear()` + rebuild nav |
| #121–123 | Session / Remember me / test session | 03 | `AppSession` in-memory; remember-me nếu cần là mở rộng |
| #124 | Models Cart, CartDetail và quan hệ | 02, 08 | carts, cart_items |
| #125–129 | Add, list, tăng/giảm, xóa, sum | 08 | CartService, transaction, recompute sum |
| #130–131 | Receiver info, checkout, create order/order_details, clear cart | 09 | JDialog + CheckoutService transaction |
| #132 | Admin quản lý Order; default PENDING; tổng tiền | 10 | OrderAdminPanel, status, detail |
| #133 | Dashboard | 11 | **Đề xuất** KPIs vì bài gốc không đặc tả số liệu |
| #134 | Admin đổi status, detail add cart, history | 10, 06 | OrderService/history, nút add tại detail |
| #136–143 | LIMIT/OFFSET, current/totalPages, prev/next | 07 | PageResult, MySQL LIMIT OFFSET |
| #144 | Product page với filter | 06–07 | Filter panel và grid/list |
| #145–150 | JPA Specifications, name search, factory/target/price | 07 | Dynamic SQL `PreparedStatement` |
| #151–156 | Kết hợp filter, lấy input, sort | 07 | Criteria object + query builder, sort whitelist |
| #159 | AJAX không reload trang | Mọi giai đoạn | `SwingWorker` + cập nhật TableModel |
| #160–161 | Constructor injection, workflow tự code | 01–12 | Chia tầng, build từng lát cắt |
| #164 | Docker server (extra) | Không bắt buộc | Desktop không cần web Docker |
| #165–168 | Fix lỗi JPA/session/Spring Tool Suite | 12 | Không bê bug Spring; kiểm thử JDBC/Swing |

## Cách phân biệt trong báo cáo
- Viết **“Kế thừa yêu cầu chức năng từ khóa học”** cho module User/Product/Auth/Cart/Order.
- Viết **“Chuyển đổi phương thức triển khai sang Swing + JDBC”** cho UI, Controller, DAO, session.
- Viết **“Nhóm chủ động bổ sung”** cho quy tắc tồn kho, transaction, enum status, BigDecimal, import ảnh an toàn, luồng kiểm thử.
- Không khẳng định đã chuyển mã nguồn dự án web: repository GitHub mới có `HelloController`, chưa có source chức năng.
