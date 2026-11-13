# KIỂM KÊ NGUỒN VÀ PHẠM VI CHUYỂN ĐỔI

## A. Hiện trạng thực tế trên GitHub
Repository: `dangquangminh123/IE303_java_storephone`; nhánh chính `main`, thời điểm kiểm tra 13/11/2026.

| Mục được xác nhận | Bằng chứng |
|---|---|
| JDK 17, Maven | `pom.xml` có `<java.version>17</java.version>` |
| Spring Boot | parent `spring-boot-starter-parent` 3.2.2, starter web |
| Entry point | `src/main/java/vn/hoidanit/laptopshop/LaptopshopApplication.java` |
| HTTP demo | `HelloController.java`, `@GetMapping("/")`, chuỗi "Hello World from Spring Boot!" |
| Port | `src/main/resources/application.properties`: `server.port=8081` |
| Lược đồ/DAO/service/controller tính năng bán hàng | **Không có trong cây tệp nhánh `main` đã kiểm tra** |
| README | Chủ yếu nói về kiến thức Java Core, Spring, SQL, Security; chưa phải đặc tả chức năng |
| Điều khoản | `LICENSE_VI.MD` yêu cầu source khóa học không được công khai |

**Kết luận đúng:** không có mã nguồn tính năng để chuyển tự động từ Spring MVC sang Swing. Ta thiết kế và lập trình lại theo tài liệu nghiệp vụ.

## B. Những yêu cầu có thực trong tài liệu Hỏi Dân IT
Tài liệu gốc nói về **website bán laptop**, với các bài:
- `#51–70`: form và CRUD User; `#72–81`: actors, models, quan hệ.
- `#83–94`: giao diện admin, ảnh upload, hash mật khẩu, CRUD User.
- `#95–106`: Product CRUD, validate, danh sách/chi tiết, trang chủ.
- `#107–123`: DTO, register, login, role, logout, session/remember-me.
- `#124–134`: Cart, CartDetail, checkout, place order, admin order, dashboard, lịch sử.
- `#136–144`: phân trang.
- `#145–156`: tìm kiếm, lọc `factory`, `target`, `price`, kết hợp nhiều tiêu chí và sort.
- `#157–168`: tổng kết, quy trình code, kiểm thử và các ghi chú sửa lỗi.

Tài liệu có nhiều mục **bài tập/`//todo` không cung cấp thuật toán hoàn chỉnh** (ví dụ dashboard #133, sort #156). Khi cần triển khai, thiết kế trong bộ file này là **đề xuất**, không phải khẳng định đó là code gốc.

## C. Bảng chuyển đổi web → desktop
| Spring MVC / web | Java Swing + JDBC | Giữ / thay |
|---|---|---|
| HTTP route `GET /products` | Nút/menu mở `ProductCatalogPanel` | Thay điểm kích hoạt |
| `@Controller`, `@GetMapping`, `@PostMapping` | Controller nhận `ActionListener` / event | Thay cách điều phối |
| JSP/JSTL/Bootstrap | `JPanel`, `JTable`, `JLabel`, `JTextField`, `JDialog` | Thay hoàn toàn view |
| `Model` truyền sang JSP | DTO + `TableModel`/setter vào panel | Giữ ý tưởng truyền dữ liệu |
| `@Service` Spring Bean | `new ProductService(productDAO,...)` | Giữ tầng service, bỏ annotation |
| Spring Data JPA `Repository` | `ProductDAO` viết SQL bằng JDBC | Thay persistence |
| `@Valid` + BindingResult | `Validator` / service trả lỗi và Swing `JOptionPane` | Giữ quy tắc validation |
| Spring Security | `AuthService` + `PasswordHasher` + `AuthorizationService` | Tự xây lại, kiểm tra ở service |
| Cookie + HttpSession + Spring Session | `AppSession` lưu user đang đăng nhập | Thay; không cần HTTP/session server |
| `MultipartFile` upload | `JFileChooser` + `Files.copy` | Thay |
| AJAX | `SwingWorker` + refresh panel/TableModel | Thay |
| Query string `?page=&sort=` | `ProductSearchCriteria` object | Thay |
| JPA Specification | `ProductDAO.search(criteria)` với SQL động có tham số | Thay |
| Pageable | `LIMIT ? OFFSET ?` và `COUNT(*)` | Thay |
| Docker/server | Chạy `.jar`/`jpackage` + MySQL | Tùy chọn đóng gói |

## D. Phân định phần yêu cầu gốc và phần cải tiến đề xuất
**Có nguồn:** GUEST / USER / ADMIN; giỏ hàng; đơn hàng; file ảnh; user/product/order; `factory`, `target`, giá; trạng thái `PENDING`; dashboard; pagination; history.

**Thêm để làm ứng dụng đúng:** `BigDecimal` thay `double`; kiểm soát hàng tồn và rollback transaction; định nghĩa trạng thái `CONFIRMED/SHIPPING/COMPLETED/CANCELLED`; phân lớp DAO; tránh thao tác DB trên Swing EDT; tìm kiếm theo tên điện thoại; giữ ảnh bằng đường dẫn tương đối; thống kê doanh thu từ `COMPLETED`; ngăn xóa dữ liệu ràng buộc.

**Chưa có yêu cầu trong nguồn, không cam kết là tính năng gốc:** thanh toán trực tuyến, vận chuyển thật, khuyến mại, OTP, email xác minh, hóa đơn điện tử, phân quyền nhân viên chi tiết, quản lý nhập hàng, phân loại bộ nhớ/màu theo SKU, tài khoản nhiều thiết bị, đồng bộ internet.

## E. Lựa chọn tên miền nghiệp vụ
Tài liệu dùng `Product` (laptop) và cột `factory` (nhà sản xuất) / `target` (mục đích sử dụng). Dự án bán điện thoại có thể:
- **Giữ tên field** `factory`, `target`, `detailDesc`, `shortDesc` ở Model để đối chiếu tài liệu thuận tiện.
- Hoặc **đổi rõ nghĩa** thành `brand`, `usagePurpose`, `detailDescription`, `shortDescription`; nên chọn cách này cho code mới, có bảng mapping trong báo cáo.
- **Không tự thêm `color/ram/storage/imei`** trừ khi đã xong chức năng bắt buộc. Nếu triển khai SKU nhiều phiên bản, phải thiết kế lại khóa tồn kho; một `products.quantity` không đại diện cho mọi màu/dung lượng.

## F. Lộ trình tái sử dụng
- Tái sử dụng: kiến thức, thiết kế nghiệp vụ, quy tắc phân quyền, mô hình quan hệ.
- Không chuyển trực tiếp: controller annotation, JSP, filter chain, Spring-specific session và method repository.
- Khuyến nghị làm **project Maven mới** (`storephone-swing`) hoặc nhánh riêng; giữ repo Spring làm bản tham khảo, tránh commit trộn hai hệ thống.
