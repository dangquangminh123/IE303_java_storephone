# GIAI ĐOẠN 05 — QUẢN LÝ SẢN PHẨM ĐIỆN THOẠI VÀ ẢNH

**Đích:** ADMIN thêm/xem/sửa/ẩn sản phẩm, upload ảnh, validation, lưu MySQL.  
**Nguồn:** #86–91 (file/preview), #95–106 (product CRUD, validate).  
**Phụ thuộc:** đăng nhập admin + `ProductDAO`/DB.

## 1. Model đúng với tài liệu — sau khi đổi tên hiển thị
| Cột tài liệu | Cột đề xuất DB | UI label | Validation |
|---|---|---|---|
| `name` | `name` | Tên điện thoại | Bắt buộc, không trống |
| `price` | `price` | Giá bán (VND) | `BigDecimal >= 0` (UI có thể yêu cầu `>0`) |
| `detailDesc` | `detail_desc` | Mô tả chi tiết | `JTextArea`, có độ dài hợp lý |
| `shortDesc` | `short_desc` | Mô tả ngắn | Tối đa 500 ký tự |
| `quantity` | `quantity` | Tồn kho | Số nguyên `>=0` |
| `factory` | `factory` | Hãng sản xuất | `Apple/Samsung/Xiaomi/...`, bắt buộc |
| `target` | `target` | Mục đích sử dụng | `GAMING/CHUP_ANH/HOC_TAP/CO_BAN` tùy seed |
| `image` | `image_path` | Ảnh chính | jpg/png/jpeg, giới hạn dung lượng |

Các tên hãng/mục đích ở trên là **dữ liệu mẫu đề xuất**. Tài liệu có `factory`/`target` nhưng ví dụ cho laptop, vì thế đừng viết mã hóa cứng chỉ cho laptop.

## 2. Màn hình quản trị
`AdminProductPanel`: bảng ID, ảnh thumbnail, tên, hãng, giá, số lượng, trạng thái; search; Create/Detail/Edit/Deactivate; pagination. `ProductFormDialog`: các ô nhập, `JTextArea`, `JComboBox`, `JSpinner` cho quantity, nút `Chọn ảnh`, preview, nút Save/Cancel.

## 3. Luồng A — Thêm mới điện thoại
1. ADMIN vào quản lý sản phẩm → `ProductController.load` → hiện JTable.
2. Bấm Thêm → `ProductFormDialog` rỗng; có placeholder ảnh.
3. Nhập tên/giá/số lượng/hãng/target/mô tả; click Chọn ảnh (không bắt buộc nếu cho phép ảnh mặc định).
4. `ProductController.onSave` → `ProductForm` DTO.
5. `ProductService.create`: `requireAdmin()` → `InputValidator.validateProduct` → `ImageStorage.saveAndReturnRelativePath` → `ProductDAO.insert`.
6. DB trả id → toast/dialog thành công → đóng form → `ProductController.reload` → bảng hiện sản phẩm.
7. Lỗi SQL sau khi copy ảnh → xóa file vừa copy (nếu file đó chưa có đối tượng nào sử dụng), giữ form và báo lỗi.

## 4. Luồng B — Import ảnh và xem trước
1. Bấm Chọn ảnh → mở `JFileChooser` filter `.png/.jpg/.jpeg` (có thể `.webp` nếu xử lý được).
2. Kiểm tra extension + nội dung thực tế bằng `ImageIO.read()` != null; giới hạn kích cỡ file và giải nén ảnh quá lớn.
3. Không dùng path do user cung cấp để tự nối vào folder upload; tạo tên file mới bằng UUID, ví dụ `products/550e8400-e29b-41d4-a716-446655440000.jpg`.
4. `Files.copy` vào thư mục ảnh cấu hình ngoài source (vd `data/images/products`); giữ DB `image_path` tương đối.
5. `ImageIO` resize preview thành thumbnail nhưng giữ tỷ lệ (không kéo giãn ảnh); `JLabel.setIcon(...)`.
6. Update ảnh: dùng file mới; sau khi DB update thành công mới cân nhắc xóa file cũ không còn ai tham chiếu. Nếu DB lỗi giữ file cũ.

## 5. Luồng C — Xem sản phẩm
1. Chọn row → `convertRowIndexToModel` → lấy `productId`.
2. Service load **bản ghi mới nhất từ DB**.
3. `ProductDetailDialog` hiển thị tên, giá định dạng VNĐ, số lượng tồn, hãng, mô tả, ảnh lớn, trạng thái.
4. Nếu không có ảnh: placeholder; nếu record bị deactivate: báo phù hợp.

## 6. Luồng D — Sửa
1. Select row → Edit → nạp current product.
2. Đổi field hợp lệ → Save.
3. `ProductService.update` kiểm tra admin + validate; gọi `ProductDAO.update WHERE id=?`.
4. Nếu có ảnh mới thì thực hiện import/file + cập nhật đường dẫn như ở B; nếu không, giữ ảnh cũ.
5. Reload bảng/catalog ngay khi lưu thành công; khi mất DB báo lỗi, không mất giá trị người dùng nhập.

## 7. Luồng E — Xóa
1. Select row → Confirm.
2. **Không hard delete** sản phẩm đã được tham chiếu order item; chỉ `UPDATE products SET active=false`.
3. Trang catalog GUEST chỉ thấy sản phẩm `active=true`; admin có thể xem cả inactive để bật lại.
4. Vẫn giữ order history với `order_items.product_name` / `unit_price` đã lưu tại thời điểm mua.

## 8. Class và method
`ProductController`, `ProductService.create/update/deactivate/findById/search`, `ProductDAO.insert/update/findById/deactivate`, `ProductFormDialog`, `ProductTableModel`, `ImageStorage.save/readThumbnail`, `ProductValidator`.

## 9. Test nghiệm thu
- [ ] Nhập tên rỗng, giá âm, quantity âm, giá là chữ → lỗi chính xác.
- [ ] Bản ghi lưu vào Workbench đủ mọi trường.
- [ ] Chọn ảnh xong thấy preview; chạy app lại vẫn thấy ảnh.
- [ ] Chọn file `.txt` giả ảnh → bị chặn.
- [ ] Đổi ảnh → bảng và dialog hiển thị đúng, không dùng cache thumbnail cũ.
- [ ] Sửa giá không thay `order_items.unit_price` đã mua.
- [ ] Soft delete không mất lịch sử, catalog ẩn sản phẩm.
- [ ] Bảng load ít nhất 20 sản phẩm và scroll tốt.

**Nghiệm thu:** tạo 3 điện thoại với 3 hãng, sửa 1, ẩn 1, mở lại app kiểm tra DB và ảnh vẫn còn.
