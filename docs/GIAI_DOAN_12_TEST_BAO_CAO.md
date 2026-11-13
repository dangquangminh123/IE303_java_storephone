# GIAI ĐOẠN 12 — TÍCH HỢP, KIỂM THỬ, ĐÓNG GÓI, BÁO CÁO CUỐI KỲ

**Đích:** phần mềm chạy lại được trên máy demo, có test/ảnh minh chứng và kịch bản thuyết trình.  
**Nguồn:** #57 (debug), #135–136, #157–168 (tổng kết/quy trình); các test cụ thể là đề xuất bổ sung.  
**Phụ thuộc:** các module bắt buộc hoàn tất.

## 1. Kiểm tra tích hợp theo luồng, không chỉ từng màn
- [ ] **Luồng GUEST:** Mở app → Home → search → filter → xem detail → add cart bị yêu cầu login.
- [ ] **Luồng USER mới:** Register → login → add A 2 lần → cart qty=2 → tăng lên 3 → checkout → đặt đơn → history xem được.
- [ ] **Luồng ADMIN:** login admin → add/sửa/ẩn điện thoại → CRUD user → xem order → thay status → dashboard cập nhật.
- [ ] **Luồng lỗi:** checkout khi admin vừa giảm stock; UI báo hết hàng, `orders/order_items/cart/products` không bị ghi nửa chừng.
- [ ] **Luồng ownership:** USER B không xem cart/order của USER A.
- [ ] **Luồng persistence:** đóng app mở lại → dữ liệu products/users/orders/cart còn; session không tự login lại.

## 2. Unit/Integration tests cần có
| Test file | Kiểm tra tối thiểu |
|---|---|
| `AuthServiceTest` | hash/login đúng và sai, role USER mặc định |
| `ProductValidatorTest` | tên rỗng, giá không hợp lệ, quantity âm |
| `ProductDAOTest` | CRUD, filter, paging dùng test DB |
| `CartServiceTest` | add trùng dòng, tăng/giảm, delete qty>1 |
| `CheckoutServiceTest` | order total, commit thành công, rollback lỗi |
| `OrderServiceTest` | ownership, status transition, hoàn kho 1 lần |
| `DashboardServiceTest` | chỉ số đúng với dữ liệu cố định |

Dùng database test riêng `storephone_test_db` hoặc fixture rollback, không test phá DB demo. Service mock DAO được khi unit test; DAO integration test với MySQL thật.

## 3. Các lỗi hay gặp và cách kiểm tra
| Vấn đề | Hướng debug |
|---|---|
| `No suitable driver` | Kiểm tra Maven dependency MySQL Connector/J; URL `jdbc:mysql://...` |
| `Access denied` | Kiểm tra username/password/quyền DB, không ghi password vào code |
| Form bị đơ | Truy vấn DB đang chạy trên EDT → chuyển `SwingWorker` |
| JTable chọn sai dòng sau sort | `convertRowIndexToModel()` |
| Ảnh mất khi đem sang máy khác | Đang lưu absolute path → dùng relative path/app-data dir |
| Tổng tiền sai | Đang dùng `double` → chuyển `BigDecimal` |
| Không thấy data mới | Quên refresh `TableModel` sau commit |
| Cart badge lệch | Recompute `SUM(cart_items.quantity)` sau các thao tác |
| Order thiếu detail | Không dùng cùng transaction/connection |
| Người dùng thường vào được admin | Check role chỉ ở UI, chưa đặt ở service |

## 4. Build và đóng gói
1. `mvn clean test`.
2. `mvn clean package`.
3. Thiết lập Main-Class `com.storephone.Main` bằng Maven manifest hoặc Shade plugin khi cần JAR kèm dependencies; nếu cần installer dùng `jpackage` của JDK.
4. Cung cấp tài liệu `README.md`: cài JDK 17, MySQL 8, chạy SQL schema, tạo admin bằng seed, cấu hình local, lệnh run JAR.
5. Cung cấp `config.example.properties`, không đóng gói mật khẩu thực; DB schema/seed demo riêng.
6. Test chạy lại **trên máy mới** hoặc user hệ điều hành mới, không dựa vào cache/đường dẫn của máy code.
7. Nếu đóng gói app, chú ý Swing app vẫn cần MySQL Server đang chạy; JAR không tự chứa MySQL DB.

## 5. Khung báo cáo cuối kỳ đề xuất
1. Giới thiệu đề tài "Ứng dụng quản lý bán điện thoại sử dụng Java Swing và MySQL".
2. Bài toán và tác nhân GUEST/USER/ADMIN; phạm vi và điều không làm.
3. Phân tích chức năng/use case; diễn giải luồng mua hàng và quản lý.
4. Thiết kế hệ thống: MVC + Service + DAO, ERD database 7 bảng.
5. Triển khai màn hình: Auth, Product, Catalog, Cart, Checkout, Orders, Dashboard.
6. Thiết kế kỹ thuật: `PreparedStatement`, BCrypt, transaction, quyền và `BigDecimal`.
7. Test case/kết quả: success/fail; bảng lỗi đã sửa.
8. Kết luận, hạn chế, hướng phát triển: phân biệt từ web Spring → desktop Swing.
9. Sơ đồ lấy từ `04_SO_DO_LUONG_NGHIEP_VU.md`, bảng truy vết `06_MA_TRAN_TRUY_VET_TAI_LIEU.md`.

## 6. Kịch bản demo 7–10 phút (có thể điều chỉnh)
- **Phút 0–1:** mở app và giải thích ba role, database MySQL.
- **Phút 1–2:** GUEST tìm kiếm/xem chi tiết.
- **Phút 2–3:** đăng ký/login USER.
- **Phút 3–5:** thêm 2 điện thoại, đổi quantity, checkout; cho xem orderId.
- **Phút 5–7:** login ADMIN, cập nhật đơn PENDING→COMPLETED, dashboard thay đổi.
- **Phút 7–8:** login USER, xem order history.
- **Phút 8–10:** trình bày ERD, transaction rollback & câu hỏi đáp.

## 7. Câu hỏi có thể bị giảng viên hỏi
- **Vì sao bỏ Spring Boot?** Đề tài yêu cầu app desktop; Swing tự vẽ UI và xử lý sự kiện không cần HTTP server, JDBC trực tiếp MySQL.
- **Còn MVC không?** Có: Model/DTO + Swing View + Controller/Service/DAO; khác cách kích hoạt bằng events thay request.
- **Sao không dùng JPA?** JDBC giúp trình bày câu SQL, khóa ngoại, transaction và truy vấn lọc trực tiếp, phù hợp học phần.
- **Làm sao tránh SQL injection?** PreparedStatement; whitelist `ORDER BY`.
- **Tại sao password không giải mã được?** BCrypt là hash một chiều, xác minh bằng `matches`.
- **Vì sao cần transaction checkout?** Tránh tạo đơn nhưng chưa trừ kho hoặc đã trừ kho nhưng giỏ chưa xóa.
- **Nếu 2 người mua sản phẩm còn 1 chiếc?** `FOR UPDATE` và điều kiện `quantity>=?`, một transaction thành công, transaction còn lại bị từ chối.
- **Làm sao quản lý trạng thái đăng nhập?** `AppSession` in-memory, logout clear; không có Spring Session/cookie.
- **Tại sao không xóa cứng product?** Giữ tham chiếu lịch sử đơn hàng, soft delete.
- **Tại sao doanh thu chỉ tính COMPLETED?** Đây là định nghĩa KPI nhóm lựa chọn và ghi rõ trong báo cáo.

## 8. Điều kiện hoàn thành dự án
- [ ] 3 role đúng, không vượt quyền.
- [ ] Sản phẩm, user, ảnh, đơn, giỏ lưu bền qua app restart.
- [ ] Checkout transaction và tồn kho chính xác.
- [ ] Có tìm/lọc/sort/phân trang.
- [ ] Có dashboard và lịch sử đơn.
- [ ] Build JAR, có hướng dẫn setup/test dễ làm lại.
- [ ] Ít nhất một báo cáo bug fix/thay đổi thiết kế.
- [ ] Mã nguồn tự viết, tôn trọng LICENSE của nguồn tham khảo.

**Không gọi hoàn thành chỉ vì chạy được UI.** Cần ít nhất một chuỗi dữ liệu thật end-to-end được kiểm tra bằng MySQL Workbench.
