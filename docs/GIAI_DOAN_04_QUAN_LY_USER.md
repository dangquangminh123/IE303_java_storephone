# GIAI ĐOẠN 04 — ADMIN QUẢN LÝ NGƯỜI DÙNG (CRUD)

**Đích:** danh sách, tạo mới, xem chi tiết, cập nhật, vô hiệu hóa User/Role.  
**Nguồn:** #51–53, #62–70, #83–94, #102 (validate), #143 (pagination).  
**Phụ thuộc:** 03 login/role; bảng `users`, `roles`.

## 1. Màn hình
`AdminUserPanel`: JTable cột `ID, Email, Họ tên, SĐT, Vai trò, Trạng thái, Ngày tạo`, thanh search email/name, nút `Thêm`, `Xem`, `Sửa`, `Vô hiệu hóa`, thanh phân trang. `UserFormDialog`: email, họ tên, phone, address, role (`JComboBox ADMIN/USER`), avatar (ảnh đại diện) và nút chọn ảnh/preview; khi tạo có password.

## 2. Luồng A — Danh sách và tìm kiếm
1. ADMIN click "Quản lý tài khoản" → `UserController.load`.
2. `AuthorizationService.requireAdmin()`.
3. `UserService.search(keyword,page,pageSize)` → `UserDAO.search` bằng `PreparedStatement` + `LIMIT/OFFSET`.
4. Tạo DTO **không chứa password hash** rồi nạp `UserTableModel`.
5. Dòng được chọn cung cấp `user.id` để xem/sửa/xóa; không lấy id bằng số thứ tự hiển thị.

## 3. Luồng B — Tạo user bởi ADMIN
1. Click Thêm → dialog rỗng.
2. Nhập email, password, họ tên, role; Validate.
3. Nếu chọn avatar: `JFileChooser` chọn ảnh jpg/png → `ImageStorage` kiểm tra nội dung và tạo preview; nếu không chọn dùng placeholder. Chỉ lưu `avatar_path` tương đối vào MySQL, tệp ảnh lưu tại `data/images/avatars`.
4. Kiểm tra email trùng; hash BCrypt; role hợp lệ.
5. `UserService.createUser()` → `UserDAO.insert()`; nếu ảnh đã copy nhưng DB lỗi, dọn file tạm.
6. Đóng dialog → reload trang, hiện bản ghi mới và thumbnail avatar.
7. Role ADMIN chỉ được set ở màn admin đã qua `requireAdmin`; register public vẫn chỉ USER.

## 4. Luồng C — Xem chi tiết
1. Chọn dòng → bấm Xem → lấy `id`.
2. `UserService.getById(id)` kiểm tra quyền rồi DAO tìm.
3. `JDialog` read-only: email, tên, địa chỉ, phone, role, trạng thái, ngày tạo; **không hiển thị password/password hash**.
4. Nếu user đã bị ẩn/xóa ở phiên khác: báo “Người dùng không tồn tại”.

## 5. Luồng D — Cập nhật
1. Chọn dòng, nhấn Sửa → load user từ DB (không tin dữ liệu JTable có thể cũ).
2. Cho phép cập nhật tên, phone, address, role, trạng thái; email có thể giữ immutable để đơn giản.
3. Nếu đổi avatar, xem trước ảnh mới; giữ nguyên ảnh cũ nếu người dùng không chọn. Không cập nhật password trong form sửa thường; tài liệu #68 cũng lưu ý bỏ password.
4. `UserService.updateUser(id, updateDTO)` validate + requireAdmin → DAO `UPDATE ... WHERE id=?`.
5. Kết quả 0 rows: thông báo record không còn; thành công reload bảng.

## 6. Luồng E — Xóa / Vô hiệu hóa
1. Chọn user → Confirm dialog.
2. Service kiểm tra **không được vô hiệu hóa tài khoản admin cuối cùng**, không xóa tài khoản đang đăng nhập nếu làm app mất quyền quản trị.
3. **Khuyến nghị soft delete:** `UPDATE users SET active=false WHERE id=?`; tài liệu gốc nói delete, nhưng soft delete giữ FK đơn hàng.
4. Cập nhật bảng, account đó login tiếp sẽ bị chặn. Nếu đang đăng nhập trên tiến trình khác, có thể cần kiểm tra `active` ở thao tác tiếp theo (mở rộng).
5. Nếu muốn hard delete chỉ user chưa có order/cart và theo quyết định rõ trong báo cáo.

## 7. Class và checklist
`UserController.onCreate/onEdit/onDeactivate`, `UserService.createUser/updateUser/deactivate/getById/search`, `UserDAO.findByEmail/findById/insert/update/deactivate/search`, `UserFormDialog`, `UserTableModel`, `ImageStorage` cho avatar.

- [ ] Validation có thông báo cụ thể và không mất input.
- [ ] Email unique, phân quyền đúng, password hash.
- [ ] Không lộ password khi xuất hiện JTable.
- [ ] Ảnh đại diện preview đúng và vẫn hiển thị sau khi khởi động lại ứng dụng.
- [ ] Sửa/xóa nhầm do sort JTable không xảy ra: dùng `convertRowIndexToModel()` khi lấy selected row.
- [ ] Đơn hàng vẫn xem được khi user bị vô hiệu hóa.
- [ ] 0 user trả empty state, không crash.
- [ ] Nút Prev/Next không vượt giới hạn.

**Nghiệm thu:** ADMIN tạo USER → User đăng nhập thử → ADMIN sửa tên/role → thử user không vào admin nếu role USER → ADMIN deactivate → login bị chặn; avatar của user còn hiển thị ở bản ghi admin.
