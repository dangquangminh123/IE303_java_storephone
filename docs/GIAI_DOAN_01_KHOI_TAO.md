# GIAI ĐOẠN 01 — KHỞI TẠO JAVA SWING, MAVEN, UI SHELL

**Đích:** ứng dụng desktop chạy từ `main`, điều hướng giữa các màn hình khung; không Spring Boot/server.  
**Nguồn nghiệp vụ:** bài #6, #10, #21–25, #43, #83–85, #160–161.  
**Phụ thuộc:** JDK 17, MySQL dự định ở đợt 02.

## 1. Việc thực hiện tuần tự
- [ ] **Bước 01 — Bảo toàn dự án cũ:** tải/clone repo để tham khảo; tạo **thư mục Maven mới** `storephone-swing` hoặc nhánh riêng. Không ghi đè thẳng project Spring gốc, tránh khó chứng minh trước/sau.
- [ ] **Bước 02 — JDK/IDE:** cài JDK 17; kiểm tra `java -version` và `javac -version`; chọn IntelliJ Community, NetBeans hoặc VS Code Java Pack.
- [ ] **Bước 03 — Maven:** tạo `pom.xml` dự án Java thường, `maven.compiler.release=17`; dependency MySQL Connector/J và JUnit; có thể thêm FlatLaf (tùy chọn). **Không** thêm `spring-boot-starter-*`, Tomcat, JSTL, JPA.
- [ ] **Bước 04 — Thư mục:** tạo gói `model`, `dto`, `dao`, `service`, `controller`, `view`, `config`, `security`, `util`, `exception` theo `02_KIEN_TRUC_MVC_SWING.md`.
- [ ] **Bước 05 — Main:** `SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));`; cấu hình close operation, kích thước mặc định, title, logo.
- [ ] **Bước 06 — Layout:** dùng **một** `JFrame` chính; header trên (logo/tên/đăng nhập/giỏ), sidebar hoặc thanh menu, nội dung `CardLayout`, footer trạng thái. Mỗi màn hình là `JPanel`; dùng `JDialog` cho create/edit/detail.
- [ ] **Bước 07 — Navigation:** đặt constants `HOME`, `LOGIN`, `REGISTER`, `CART`, `ADMIN_DASHBOARD`, `ADMIN_USERS`, `ADMIN_PRODUCTS`, `ADMIN_ORDERS`, `ORDER_HISTORY`; viết `NavigationController.show(String route)`.
- [ ] **Bước 08 — AppContext:** tạo singleton/lớp startup chỉ để lắp ghép dependencies; view nhận controller qua constructor, không tự mở DB.
- [ ] **Bước 09 — UI component:** chuẩn hóa font hiển thị Unicode tiếng Việt, màu, padding, `JScrollPane` cho bảng; hiển thị tiêu đề cửa sổ, empty state, loading state.
- [ ] **Bước 10 — Commit:** `git add .`, commit `feat: swing app shell`, không commit file password hoặc `.class`.

## 2. Luồng A — Mở ứng dụng
`Main.main` → khởi tạo theme/nguồn ảnh → `MainFrame` → đăng ký các panel → `show(HOME)` → khách thấy danh sách trống/placeholder hợp lệ. Chưa kết nối DB ở bước này; có thể tạo dữ liệu mock chỉ cho kiểm tra bố cục, **phải bỏ khi nối DB thật**.

## 3. Luồng B — Chuyển màn hình
`JButton "Đăng nhập"` → `ActionListener` → `NavigationController.show(LOGIN)` → `CardLayout.show()` → panel `LoginPanel`; bấm "Trang chủ" trở về `HOME`; không mở thêm nhiều frame gây trùng state. Các màn hình ADMIN có thể dựng placeholder nhưng **chưa cấp quyền cho GUEST**.

## 4. Luồng C — Hiện trạng thái phiên
Header đọc `AppSession.isAuthenticated()` → nếu GUEST hiện Login/Register và ẩn Cart/User; nếu USER hiện tên và Cart/History/Logout; nếu ADMIN hiện link quản trị. Ở giai đoạn này có thể dùng giả lập `AppSession`, đến giai đoạn 03 thay bằng session thật.

## 5. Class/method nên có
| Class | Method/trách nhiệm |
|---|---|
| `Main` | `main(String[])` |
| `MainFrame` | `registerScreen`, `showScreen`, `updateHeaderForSession` |
| `NavigationController` | `showHome`, `showLogin`, `showAdmin`, `showCart` |
| `AppContext` | khởi tạo service/controller, cung cấp constructor dependencies |
| `UiTaskRunner` | bọc `SwingWorker` cho công việc lâu (sẽ dùng đợt sau) |

## 6. Điều kiện nghiệm thu
- [ ] `mvn clean package` chạy qua không cần Spring server.
- [ ] Mở cửa sổ desktop, nhấn menu đổi đúng ít nhất 4 panel.
- [ ] Đóng app không để tiến trình/DB kết nối treo.
- [ ] Không gọi SQL trong `ActionListener`.
- [ ] Không có `@RestController`, `.jsp`, `server.port` trong project Swing.

**Kết quả phải lưu:** ảnh chụp home shell và cây thư mục Maven.
