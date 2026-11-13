# CHECKLIST NGHIỆM THU THEO TỪNG ĐỢT — STOREPHONE SWING

> Sao chép file này vào repo code của bạn, tích `[x]` sau khi có **minh chứng thực thi**, không đánh dấu theo cảm giác. Mỗi đợt tương ứng một commit/tag và một ảnh/video demo.

## 1. Checklist tiến độ 12 đợt

### Đợt 01 — Khởi tạo
- [x] P01-01: JDK 17, Maven desktop Java không còn Spring Boot.
- [x] P01-02: `MainFrame` một JFrame, các JPanel điều hướng được bằng CardLayout.
- [x] P01-03: AppContext, Controller, Service, DAO package được tạo đúng.
- [ ] P01-04: Git commit và screenshot giao diện khung.

### Đợt 02 — MySQL & DAO
- [x] P02-01: Schema 7 bảng, role seed, PK/FK/UNIQUE/CHECK đầy đủ.
- [x] P02-02: JDBC ConnectionFactory, config nằm ngoài Git.
- [x] P02-03: CRUD model bằng PreparedStatement, map ResultSet.
- [x] P02-04: Test duplicate/FK/rollback/query lỗi.

### Đợt 03 — Auth
- [x] P03-01: Register USER, validate email/password, email unique.
- [x] P03-02: BCrypt hash+verify, login thành công/sai.
- [x] P03-03: `AppSession` login/logout, header đúng role.
- [x] P03-04: ADMIN-only service bị chặn với USER.

### Đợt 04 — User management
- [x] P04-01: ADMIN list/search/page users.
- [x] P04-02: CREATE user theo role + hash password.
- [x] P04-03: READ detail không lộ password.
- [x] P04-04: UPDATE profile/role, không thay password từ form thường.
- [x] P04-05: DEACTIVATE an toàn, đơn cũ giữ nguyên.
- [x] P04-06: Upload/preview avatar user, lưu path tương đối, mở app lại vẫn thấy ảnh.

### Đợt 05 — Product + ảnh
- [x] P05-01: ADMIN form có name/price/shortDesc/detailDesc/quantity/factory/target/image.
- [x] P05-02: Validate giá/số lượng/tên/mô tả.
- [x] P05-03: CREATE/READ/UPDATE/DEACTIVATE product.
- [x] P05-04: `JFileChooser`, preview, copy ảnh/relative path.
- [x] P05-05: Reload app ảnh còn, giá/tồn còn.

### Đợt 06 — Catalog
- [x] P06-01: GUEST xem list điện thoại.
- [x] P06-02: Chi tiết có ảnh/giá/mô tả/hãng/stock.
- [x] P06-03: Product inactive không hiển thị ở catalog.
- [x] P06-04: GUEST click add cart phải login.
- [x] P06-05: Sau update product, catalog reload đúng.

### Đợt 07 — Search/Filter/Sort/Pagination
- [x] P07-01: keyword tìm theo tên.
- [x] P07-02: IN nhiều hãng/target; price min/max/range.
- [x] P07-03: tổ hợp filter, COUNT và DATA cùng WHERE.
- [x] P07-04: sort whitelist giá/tên/ngày; không SQL injection.
- [x] P07-05: next/prev/first/last, page clamp, zero result.
- [x] P07-06: giữ state filter sau xem detail.

### Đợt 08 — Cart
- [x] P08-01: cart riêng theo current USER.
- [x] P08-02: add trùng product thì tăng quantity cùng row.
- [x] P08-03: list/cart total tính BigDecimal chính xác.
- [x] P08-04: tăng/giảm/xóa cập nhật total_quantity đúng.
- [x] P08-05: qty > stock, qty <= 0 bị chặn.
- [x] P08-06: logout/login lại cùng user còn giỏ.

### Đợt 09 — Checkout
- [x] P09-01: preview total, receiver name/address/phone.
- [x] P09-02: service validate form, userId lấy từ session.
- [x] P09-03: transaction tạo Order/OrderItem/trừ stock/xóa Cart.
- [x] P09-04: PENDING và total_amount được lưu.
- [x] P09-05: simulate lỗi giữa chừng rollback toàn bộ.
- [x] P09-06: double click confirm không tạo 2 order.

### Đợt 10 — Order & History
- [x] P10-01: ADMIN list/detail tất cả orders, không tạo order từ ADMIN.
- [x] P10-02: USER list/detail **chỉ order của mình**.
- [x] P10-03: update trạng thái theo state machine.
- [x] P10-04: cancel hoàn tồn đúng một lần.
- [x] P10-05: history dùng giá snapshot, không bị đổi theo product.

### Đợt 11 — Dashboard
- [x] P11-01: tổng users/products active.
- [x] P11-02: pending orders.
- [x] P11-03: revenue chỉ từ COMPLETED (quy ước đề xuất).
- [x] P11-04: recent orders/top sales (nếu làm).
- [x] P11-05: refresh thay đổi số liệu, chỉ ADMIN xem.

### Đợt 12 — Test & Báo cáo
- [x] P12-01: JUnit tests quan trọng đều pass.
- [x] P12-02: flow GUEST→USER→ADMIN→USER end-to-end.
- [ ] P12-03: Ảnh ERD, layer architecture, screenshot màn hình.
- [x] P12-04: Build jar và chạy lại trên máy khác.
- [x] P12-05: README setup JDK/MySQL/seed/run.
- [x] P12-06: Báo cáo nêu nguồn gốc nghiệp vụ, phần bổ sung, giới hạn.
- [x] P12-07: Không public mã nguồn khóa học trái điều khoản.

## 2. Bộ dữ liệu test để chấm chéo
| ID case | Input/Thao tác | Kết quả mong đợi |
|---|---|---|
| TC-01 | Register user@gmail.com | User được thêm role USER, hash BCrypt |
| TC-02 | Register email trùng | Báo trùng, DB không thêm |
| TC-03 | Login sai password | Không tạo session |
| TC-04 | USER gọi ProductService.create | AuthorizationException |
| TC-05 | ADMIN add Product giá -1 | Lỗi validation, không ghi |
| TC-06 | GUEST mở Home | Có list active, không đòi login |
| TC-07 | Search keyword không tồn tại | Empty state, total=0 |
| TC-08 | Lọc factory 2 hãng + range giá | Các row thỏa AND các nhóm/OR trong nhóm |
| TC-09 | Sort price desc | Giá giảm, ổn định theo id |
| TC-10 | PageSize=10, 11 records | 2 trang: 10 + 1 |
| TC-11 | USER add A hai lần | 1 cart_item A quantity=2 |
| TC-12 | USER thêm B qty=4 rồi xóa B | Tổng số lượng giảm 4 |
| TC-13 | USER login lại | Giỏ được khôi phục từ MySQL |
| TC-14 | USER checkout giỏ rỗng | Không tạo order |
| TC-15 | ReceiverPhone trống | Lỗi, không ghi order |
| TC-16 | Checkout A qty=2 kho=3 | Order tạo, kho còn 1 |
| TC-17 | Checkout A qty=2 kho=1 | Rollback, kho/giỏ/order nguyên |
| TC-18 | Mock SQL error sau insert orders | Không còn order orphan |
| TC-19 | USER B xem order của USER A | Access denied |
| TC-20 | ADMIN hủy đơn chưa giao | Hoàn kho + trạng thái CANCELLED |
| TC-21 | ADMIN hủy lại đơn đã CANCELLED | Không hoàn kho lần hai |
| TC-22 | Sửa Product.price sau đơn | Order detail giữ giá cũ |
| TC-23 | Đổi trạng thái sang COMPLETED | Dashboard revenue cập nhật |
| TC-24 | App restart | DB còn; AppSession logout |

## 3. Tiêu chí ưu tiên nếu thiếu thời gian
**Bắt buộc**: P01–P06 + P08–P10 ở mức tối thiểu, có DB/permission/transaction đúng.  
**Rất nên có**: P07 search/filter/page và P11 dashboard (trực tiếp từ tài liệu).  
**Có thể để hướng phát triển**: thanh toán online, nhập hàng, barcode/IMEI, nhiều biến thể SKU, in PDF hóa đơn, xuất Excel, socket realtime.

## 4. Định nghĩa "hoàn thành một ticket"
Một ticket chỉ hoàn thành khi có đủ:
1. View hoặc API service liên quan thao tác được.
2. Data ghi/đọc thật ở MySQL.
3. Case thành công và tối thiểu một case lỗi đều chạy đúng.
4. Không vượt phân quyền hoặc vi phạm ràng buộc nghiệp vụ.
5. Commit rõ tên, không có log/mật khẩu nhạy cảm.
6. Ảnh hoặc video minh chứng lưu vào tài liệu nhóm.

## 5. Chỉ số tổng quan
- 12 đợt.
- 7 bảng DB.
- 3 vai trò hiển thị: GUEST (không ghi DB), USER, ADMIN.
- 24 test case tiêu biểu + test chi tiết ở từng file giai đoạn.
- Mục tiêu cuối: một chuỗi trải nghiệm có thể trình bày và kiểm tra trên MySQL.
