# GIAI ĐOẠN 03 — ĐĂNG KÝ, ĐĂNG NHẬP, ĐĂNG XUẤT, PHÂN QUYỀN

**Đích:** GUEST/USER/ADMIN với quyền đúng, mật khẩu hash, session desktop rõ ràng.  
**Nguồn:** #38–39, #92–93, #107–123.  
**Phụ thuộc:** 01, 02; 2 role đã seed.

## 1. Màn hình phải có
- `LoginPanel`: email, mật khẩu `JPasswordField`, nút Login, link Register.
- `RegisterPanel`: email, mật khẩu, xác nhận mật khẩu, họ tên, phone, address.
- Header chung: trạng thái login, tên người dùng, nút Logout, shortcut cart/history/admin.
- Dialog lỗi: thông tin thiếu, email trùng, credentials sai, account khóa, không đủ quyền.

## 2. Luồng A — Đăng ký (`Register`)
1. GUEST mở Register → điền form → click "Tạo tài khoản".
2. `AuthController.register()` đọc field; dùng `char[]` cho password, xóa nội dung nhạy cảm khỏi bộ nhớ khi có thể.
3. `AuthService.register(RegisterRequest)` validate email (cú pháp + trim/lowercase), tên không rỗng, password có chiều dài theo quy tắc chốt, xác nhận khớp, phone hợp lệ.
4. `UserDAO.findByEmail` kiểm tra trùng (vẫn **bắt duplicate key** tại SQL để tránh race).
5. `PasswordHasher.hash()` tạo BCrypt, gán role **USER** từ `RoleDAO.findByName("USER")`; **không cho nhập role ADMIN trong form public**.
6. `UserDAO.insert()` → thông báo thành công → điều hướng Login (hoặc auto login nếu tự chọn, nên về Login để dễ demo).
7. Khi lỗi: giữ lại các trường không nhạy cảm, xóa trường password, hiện message chính xác.

## 3. Luồng B — Login
1. GUEST nhập email/password.
2. `AuthController` → `AuthService.login`.
3. `UserDAO.findByEmail` → kiểm tra user tồn tại và `active=true`.
4. `PasswordHasher.matches` so mật khẩu với BCrypt; không hash password nhập rồi so chuỗi trực tiếp (BCrypt có salt).
5. Thành công → tạo `UserSession(id, name, role)` chỉ chứa dữ liệu cần thiết → `AppSession.setCurrentUser`.
6. Update header/menu + cart badge; ADMIN → `AdminDashboardPanel`, USER → Home.
7. Thất bại → thông báo chung "Email hoặc mật khẩu không đúng" để tránh lộ sự tồn tại của email; không lưu password vào log.

## 4. Luồng C — Phân quyền cả ở UI và Service
| Chức năng | GUEST | USER | ADMIN |
|---|---|---|---|
| Xem danh sách, chi tiết, search/filter | ✅ | ✅ | ✅ |
| Register/login | ✅ | N/A | N/A |
| Giỏ hàng, checkout, lịch sử bản thân | ❌ | ✅ | ✅ |
| CRUD user | ❌ | ❌ | ✅ |
| CRUD product, image | ❌ | ❌ | ✅ |
| Duyệt trạng thái order, dashboard | ❌ | ❌ | ✅ |

- View: ẩn menu ADMIN khi không có role ADMIN, ẩn Cart trước login.
- Service: `authorization.requireLoggedIn()` hoặc `requireAdmin()` **được gọi ở đầu method**, không chỉ dựa vào nút bị ẩn.
- Nếu có ai truy cập panel trực tiếp khi role không đủ: chuyển về Home hoặc hiện Access Denied.
- ADMIN có thể mua thử như USER; nếu không cần, giới hạn cho USER bằng quy tắc rõ ràng, không tùy tiện lúc test.

## 5. Luồng D — Logout / hủy phiên
1. USER/ADMIN click Logout → `AuthService.logout`.
2. `AppSession.clear`; bỏ thông tin user/role/cart badge khỏi RAM.
3. Xóa dữ liệu nhạy cảm còn trong panel, render lại menu.
4. Về Home dưới vai trò GUEST; mở Cart/Checkout sau logout phải chặn.
5. **Không tự động đăng nhập lại bằng cookie**. Tính năng Remember Me là kiến thức web (#121–123); nếu muốn thêm cho desktop, phải thiết kế cơ chế lưu an toàn riêng, không lưu plaintext.

## 6. Class/method cần có
`UserSession`, `AppSession`, `PasswordHasher`, `AuthController`, `AuthService`, `AuthorizationService`, `LoginRequest`, `RegisterRequest`, `InputValidator`, `UserDAO`, `RoleDAO`, `LoginPanel`, `RegisterPanel`.

Gợi ý `AuthService.login(email, char[]) -> UserSession`; `AppSession.currentUser() -> Optional<UserSession>`; `AuthorizationService.requireAdmin()`; `PasswordHasher.hash/verify`.

## 7. Test bắt buộc
- [ ] Email sai/mật khẩu sai đều thất bại và không vào session.
- [ ] USER login thành công, không mở được admin product bằng đường điều hướng khác.
- [ ] ADMIN vào dashboard/CRUD được.
- [ ] Register mới tạo role USER dù client cố nhập "ADMIN".
- [ ] Password DB là hash BCrypt; không có mật khẩu gốc.
- [ ] Logout và login tài khoản khác không lộ giỏ/lịch sử account cũ.
- [ ] Tắt app/mở lại là logout (vì session trong RAM).
- [ ] `active=false` không đăng nhập được.

## 8. Điều kiện nghiệm thu
Demo: Register → Login USER → truy cập Cart → Logout → Cart bị chặn → Login ADMIN → vào Dashboard → USER bị từ chối khi gọi chức năng quản trị. Chụp ảnh bảng `users` (ẩn hash khi trình chiếu).
