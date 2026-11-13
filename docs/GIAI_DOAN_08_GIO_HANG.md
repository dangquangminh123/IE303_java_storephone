# GIAI ĐOẠN 08 — GIỎ HÀNG (CART / CART_ITEM)

**Đích:** giỏ hàng persistent của USER, không trùng dòng, tăng/giảm/xóa chính xác.  
**Nguồn:** #124–129, #134; bản gốc gọi `CartDetail`.  
**Phụ thuộc:** auth, Product, MySQL schema.

## 1. Màn hình
`CartPanel`: JTable ảnh/tên/giá đơn vị/số lượng/thành tiền; nút `+`, `−`, `Xóa`, `Tiếp tục mua`, `Thanh toán`; hiển thị `Tổng số lượng` và `Tổng tiền`. Khi rỗng: "Giỏ hàng của bạn đang trống".
Header có cart badge lấy từ `CartService.getTotalQuantity(currentUserId)`.

## 2. Luồng A — Add to cart từ danh sách/chi tiết
1. GUEST click → chặn, dẫn tới Login; USER click `Thêm vào giỏ` với productId và qty (mặc định 1).
2. Controller → `CartService.add(productId,qty)`.
3. Service `requireLoggedIn`, `qty>0`, query Product để kiểm tra active, đủ stock.
4. DAO tạo `Cart` nếu user chưa có. Quan hệ **User 1:0..1 Cart**.
5. Trong transaction, `CartDAO` tìm dòng `(cart_id,product_id)`:
   - **Chưa có:** INSERT `cart_items(product_id,quantity,unit_price)`.
   - **Có:** cộng quantity hiện tại + qty, xác nhận không vượt stock (đừng tạo dòng mới).
6. Recompute `carts.total_quantity = SUM(cart_items.quantity)`.
7. Commit → UI thông báo → cập nhật badge và CartPanel; nếu lỗi rollback.
8. Giá `unit_price` trong cart: chọn quy ước **chốt lúc thêm giỏ**, nếu giá sản phẩm thay đổi trước checkout phải có chính sách rõ; bản kế hoạch cho bài cuối kỳ **giữ snapshot giá tại cart, hiển thị đúng tổng từ snapshot**.

## 3. Luồng B — Hiển thị chi tiết cart
1. USER mở Cart → `CartService.getCurrentCart()` lấy `AppSession.currentUserId`.
2. `CartDAO.findByUserId`, có thể không tồn tại → trả `CartView` rỗng, tổng=0.
3. Query JOIN `cart_items` + `products`, mapping thành rows.
4. Mỗi row thành tiền `unit_price × quantity`; tổng tiền dùng `BigDecimal.add()`.
5. Render JTable; id row phải tham chiếu item/product thật, không dùng index UI làm ID.

## 4. Luồng C — Tăng / giảm
1. User click `+` → gọi `CartService.setQuantity(productId, oldQty+1)`.
2. Validate >0, <= stock; update đúng dòng sở hữu user; recompute tổng + refresh.
3. Click `−` khi oldQty>1 → giảm 1.
4. Click `−` khi oldQty=1 → hỏi có muốn xóa khỏi giỏ hay giữ 1; **chọn xóa sau xác nhận** để dễ demo.
5. Không chấp nhận qty thập phân/âm/zero trong `cart_items`.

## 5. Luồng D — Xóa một sản phẩm khỏi giỏ
1. Chọn row → nút `Xóa` → confirm.
2. Service xác định dòng thuộc cart **của currentUser** (không chỉ tin `cartItemId`).
3. `DELETE FROM cart_items WHERE id=? AND cart_id=?`.
4. Recompute tổng bằng SUM; nếu cart rỗng có thể giữ cart với `total_quantity=0` hoặc xóa cart. **Chọn giữ cart rỗng** để đơn giản, vì table cho phép Cart 1:0..N.
5. Commit → refresh UI/badge; nếu 0 rows, báo dòng đã được xóa/không hợp lệ.

**Khác biệt được sửa từ bài #129:** xóa dòng qty=4 phải trừ 4 khỏi tổng, không chỉ trừ 1. Vì thế luôn recompute SUM.

## 6. Luồng E — Logout/login lại
1. Giỏ lưu MySQL nên khi logout chỉ reset badge UI về 0, **không xóa giỏ DB**.
2. Login lại cùng USER → `CartService.getCurrentCart()` nạp lại đúng items.
3. Login USER khác → chỉ thấy Cart của chính user khác; tuyệt đối không rò dữ liệu theo session trước.

## 7. Class/method
`Cart`, `CartItem`, `CartView`, `CartRowDTO`, `CartDAO.findOrCreateByUser/findItems/upsert/delete/recompute`, `CartService.add/setQuantity/remove/getCurrentCart/getTotalQuantity`, `CartController`, `CartPanel`, `CartTableModel`.

## 8. Test nghiệm thu
- [ ] GUEST không add cart.
- [ ] Add sản phẩm A hai lần → **1 row** qty=2 và badge=2.
- [ ] Add thêm B qty=3 → badge=5.
- [ ] Xóa B → badge=2, không phải 4.
- [ ] Cố tăng A vượt tồn → từ chối, DB không bị cập nhật nửa vời.
- [ ] Số lượng 0/âm → bị chặn.
- [ ] Cart rỗng không null pointer/không checkout được.
- [ ] Logout/login cùng account còn giỏ; account khác không nhìn thấy.
- [ ] Tắt/mở phần mềm giỏ vẫn giữ.
- [ ] Tổng tiền = tổng `unit_price×quantity` của các dòng.

**Nghiệm thu:** add 2 sản phẩm, đổi qty, xóa 1, logout/login → giỏ đúng như trước khi thoát.
