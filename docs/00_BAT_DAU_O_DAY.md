# LỘ TRÌNH XÂY DỰNG PHẦN MỀM BÁN ĐIỆN THOẠI — JAVA SWING + MYSQL

> Dự án cuối kỳ: **IE303_java_storephone**. Bản kế hoạch kỹ thuật, không phải mã nguồn đã triển khai. Cập nhật 13/11/2026.

## 1. Đã kiểm tra những gì?
- GitHub: https://github.com/dangquangminh123/IE303_java_storephone (nhánh `main`).
- Tài liệu tham chiếu: `[PUBLIC] Spring MVC - hoidanit(2).txt`, Hỏi Dân IT, version 6.0.
- Repository hiện tại chỉ có **khởi tạo Spring Boot**: `HelloController.java` với route GET `/`, `LaptopshopApplication.java`, `application.properties` (`server.port=8081`), Maven `pom.xml` (Spring Boot 3.2.2 / Java 17), các file README và LICENSE. **Chưa có hệ thống bán hàng hay các module tương ứng trong code repo**.
- Tài liệu mô tả **website bán laptop**; bài cuối kỳ này chuyển nghiệp vụ chung sang **phần mềm bán điện thoại chạy desktop**. Không hiểu nhầm tên repo là bằng chứng đã có chức năng bán điện thoại.

## 2. Kết quả cuối cùng cần trình diễn
Ứng dụng Java Swing mở bằng `main()` và kết nối MySQL. Có ba nhóm người dùng:
1. **GUEST** (chưa đăng nhập): xem danh sách, tìm kiếm/lọc, xem chi tiết điện thoại; không đặt hàng.
2. **USER**: đăng ký, đăng nhập, xem sản phẩm, quản lý giỏ, đặt đơn, xem lịch sử mua.
3. **ADMIN**: quản lý user, sản phẩm/ảnh, đơn hàng, trạng thái đơn, dashboard; cũng có thể dùng chức năng của USER tùy quy ước.

**Phạm vi cốt lõi bám tài liệu:** User/Role, Product, Image, Auth, Cart, Checkout, Order/OrderDetail, History, Dashboard, Pagination, Filter, Sort. **Mở rộng do bản kế hoạch đề xuất:** tồn kho an toàn trong transaction, trạng thái đơn rõ ràng, nhật ký lỗi, giao diện chuyên biệt cho điện thoại, chạy nền bằng `SwingWorker`, test và đóng gói.

## 3. Thứ tự triển khai bắt buộc
| Đợt | Tập trung | Tệp thực hiện | Mốc ra sản phẩm |
|---|---|---|---|
| 01 | Maven Java 17 + Swing app shell + Git | `GIAI_DOAN_01_KHOI_TAO.md` | Cửa sổ Swing hoạt động, điều hướng không lỗi |
| 02 | Thiết kế MySQL + JDBC + DAO | `GIAI_DOAN_02_CSDL_DAO.md` | CRUD qua database và test kết nối |
| 03 | Đăng ký/đăng nhập/đăng xuất/role | `GIAI_DOAN_03_AUTH_PHAN_QUYEN.md` | ADMIN/USER đăng nhập đúng quyền |
| 04 | CRUD người dùng | `GIAI_DOAN_04_QUAN_LY_USER.md` | Admin quản lý tài khoản |
| 05 | CRUD sản phẩm + ảnh + validation | `GIAI_DOAN_05_SAN_PHAM_ANH.md` | Admin quản lý điện thoại |
| 06 | Trang chủ, danh sách, chi tiết | `GIAI_DOAN_06_TRANG_CHU_CHI_TIET.md` | Khách xem điện thoại |
| 07 | Search, filter, sort, pagination | `GIAI_DOAN_07_TIM_KIEM_LOC_SAP_XEP_PHAN_TRANG.md` | Tìm kiếm có nhiều tiêu chí |
| 08 | Cart và CartItem | `GIAI_DOAN_08_GIO_HANG.md` | USER thêm/sửa/xóa giỏ |
| 09 | Checkout / Place Order | `GIAI_DOAN_09_CHECKOUT_DAT_HANG.md` | Đặt đơn được và trừ tồn kho |
| 10 | Admin orders + lịch sử khách | `GIAI_DOAN_10_DON_HANG_LICH_SU.md` | Tra cứu & xử lý đơn |
| 11 | Dashboard tổng quan | `GIAI_DOAN_11_DASHBOARD.md` | Thống kê đúng từ database |
| 12 | Tích hợp / kiểm thử / đóng gói / báo cáo | `GIAI_DOAN_12_TEST_BAO_CAO.md` | Bản demo cuối kỳ có thể chạy lại |

**Đừng làm song song tất cả màn hình.** Thực hiện theo từng lát cắt: nút → controller → service → DAO → MySQL → tải lại giao diện → kiểm thử. Kết thúc đợt nào phải commit và demo được đợt đó.

## 4. Các tài liệu nền phải đọc trước khi code
- `01_DOI_CHIEU_NGUON_VA_PHAM_VI.md`: cái gì có thật trên GitHub/tài liệu, cái gì là lựa chọn chuyển đổi.
- `02_KIEN_TRUC_MVC_SWING.md`: gói/class, cách truyền dữ liệu, xử lý thread và quyền.
- `03_THIET_KE_DATABASE_MYSQL.md`: ERD, cột, khóa ngoại và SQL khởi tạo.
- `04_SO_DO_LUONG_NGHIEP_VU.md`: sơ đồ nghiệp vụ có thể đưa vào báo cáo.
- `05_CHECKLIST_TONG_HOP.md`: checklist nghiệm thu cuối cùng.
- `06_MA_TRAN_TRUY_VET_TAI_LIEU.md`: đối chiếu chương/bài gốc đến đợt xây dựng.
- `07_MAU_CODE_KHOI_DONG.md`: ví dụ pom.xml, Swing main, JDBC, BCrypt, SwingWorker để bắt đầu (tùy chọn).

## 5. Quyết định kỹ thuật chốt cho bản cuối kỳ
| Chủ đề | Lựa chọn |
|---|---|
| Ngôn ngữ | Java 17 |
| Desktop UI | Swing (`JFrame`, `JPanel`, `JTable`, `JDialog`, `CardLayout`) |
| Build | Maven, **không dùng Spring Boot** |
| Database | MySQL 8+; MySQL Workbench để quản trị |
| Truy cập DB | JDBC + `PreparedStatement`, DAO thủ công |
| Kiến trúc | Model + View + Controller + Service + DAO |
| Số tiền | `BigDecimal` bên Java / `DECIMAL(15,0)` bên SQL |
| Mật khẩu | BCrypt, **không lưu plaintext** |
| Giữ trạng thái đăng nhập | `AppSession` trong RAM tiến trình (không dùng Spring Session/cookie) |
| Ảnh | Chép file vào thư mục ứng dụng, database lưu đường dẫn tương đối |
| Kiểm thử | JUnit 5 cho service/DAO quan trọng + test tay theo checklist |
| Cấu hình | `config.properties` ở local, mật khẩu DB không commit |

## 6. Thời gian tham khảo (điều chỉnh theo tiến độ)
- Ngày 1–2: Đợt 01–02.
- Ngày 3–4: Đợt 03–04.
- Ngày 5–6: Đợt 05–06.
- Ngày 7: Đợt 07.
- Ngày 8–9: Đợt 08–09.
- Ngày 10: Đợt 10–11.
- Ngày 11–12: Đợt 12 + hoàn thiện tài liệu bảo vệ.

Đây chỉ là **kế hoạch gợi ý**, không phải cam kết tiến độ. Nếu thời gian gấp, ưu tiên **luồng trọn vẹn đăng nhập → thêm sản phẩm → giỏ hàng → đặt hàng → admin duyệt → lịch sử** hơn là trang trí giao diện.

## 7. Về giấy phép của repository gốc
Trong repo có `LICENSE_VI.MD` nêu rằng mã nguồn khóa học chỉ được sử dụng học tập cá nhân và **không chia sẻ mã nguồn khóa học ở repository công khai**. Repo gốc hiện đang public. Hãy kiểm tra quyền sử dụng, tránh đăng lại mã nguồn khóa học; nếu chứa source được cấp riêng cho học viên, chuyển repo phù hợp sang PRIVATE. Bộ Markdown này là **lộ trình và thiết kế mới do mình xây dựng**, không sao chép source khóa học.

## 8. Cách sử dụng các file
1. Đọc 00–04 để hiểu cách chuyển công nghệ.
2. Mở đúng tệp `GIAI_DOAN_XX...md`, làm theo thứ tự **bước triển khai**.
3. Đánh dấu từng ô `[x]` khi đã chạy được, chụp ảnh minh chứng.
4. Chỉ chuyển sang đợt kế tiếp nếu hoàn thành phần **Điều kiện nghiệm thu**.
5. Khi demo cuối kỳ, dùng `04_SO_DO_LUONG_NGHIEP_VU.md` để kể luồng người dùng, không chỉ liệt kê class.
