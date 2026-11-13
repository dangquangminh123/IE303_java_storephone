# GIAI ĐOẠN 09 — CHECKOUT / PLACE ORDER (TRANSACTION)

**Đích:** USER đặt đơn thực sự, đơn có chi tiết/giá/tổng, kho trừ đúng, giỏ được làm trống khi thành công, lỗi rollback toàn bộ.  
**Nguồn:** #130–131; `receiverName/receiverAddress/receiverPhone`, trạng thái `PENDING`, `OrderDetail`.  
**Phụ thuộc:** Cart và Product hoạt động.

## 1. UI của CheckoutPanel
- Bảng mặt hàng lấy từ giỏ (read-only khi ở bước xác nhận); cột `Tên, SL, Đơn giá, Thành tiền`.
- Form `Họ tên người nhận`, `Số điện thoại`, `Địa chỉ nhận hàng` (tự điền từ profile nếu có nhưng được sửa).
- Tổng tiền VND, chú thích **"Đặt hàng mô phỏng — chưa tích hợp thanh toán thực tế"**.
- Nút `Xác nhận đặt hàng`, `Quay lại giỏ`.

## 2. Luồng A — Mở checkout
1. USER vào Cart, cập nhật qty đến mức mong muốn.
2. Click `Thanh toán` → controller `openCheckout()`.
3. `CheckoutService.preview()` kiểm tra session có user, giỏ không rỗng, tính tổng từ `cart_items.unit_price * quantity`.
4. Hiển thị các dòng và thông tin người nhận; không tạo order ngay ở bước này.
5. Nếu giỏ rỗng → hiện thông báo và trở lại Cart.

## 3. Luồng B — Validate form nhận hàng
1. Nhấn Xác nhận, disable nút tránh double-click.
2. Service bắt buộc `receiverName`, `receiverPhone`, `receiverAddress` không trống; giới hạn độ dài; kiểm tra phone theo quy tắc demo và chống nhập toàn khoảng trắng.
3. Không lấy `user_id` từ input form; dùng `AppSession.currentUserId`.
4. UI không truyền tổng tiền như một giá trị "tin cậy"; **service tự tính lại từ DB**, người dùng không sửa được tổng qua form.
5. Nếu lỗi → giữ form, highlight ô, chưa ghi order.

## 4. Luồng C — Giao dịch đặt đơn nguyên tử
**Không tách làm nhiều connection.**
1. Service yêu cầu login, mở `Connection`, `setAutoCommit(false)`.
2. Lock các dòng `cart_items` liên quan của user và `products` theo productId tăng dần để tránh deadlock khi có nhiều người checkout.
3. Kiểm tra giỏ vẫn còn, mỗi product `active=true`, kho đủ qty, giá cart snapshot hợp lệ; nếu có thay đổi cảnh báo/từ chối và yêu cầu refresh.
4. Tính `total = Σ(unit_price × qty)` bằng BigDecimal.
5. INSERT vào `orders`: `user_id`, receiver info, `status=PENDING`, `total_amount=total`, `created_at`.
6. Lấy khóa order mới bằng JDBC `Statement.RETURN_GENERATED_KEYS`.
7. INSERT mỗi `order_items`: `order_id`, `product_id`, `product_name`, `quantity`, `unit_price` (snapshot).
8. Với mỗi product: `UPDATE products SET quantity=quantity-? WHERE id=? AND active=TRUE AND quantity>=?`; nếu affected rows !=1 → throw `InsufficientStockException`, rollback.
9. DELETE các `cart_items` đã checkout; update `carts.total_quantity=0` (hoặc xóa carts theo quy ước; ở đây giữ cart rỗng).
10. `COMMIT` toàn bộ; trả `orderId`.
11. Nếu bất kỳ lỗi nào → `ROLLBACK`, không có order nửa vời, không trừ kho nửa vời, giỏ giữ nguyên; `finally` khôi phục autocommit/đóng connection.
12. UI bật lại nút, hiện mã đơn + trạng thái `PENDING`, reset cart badge, chuyển lịch sử mua.

## 5. Trường hợp nâng cao: mua một phần giỏ
Tài liệu #131 có hai case. **MVP chọn thanh toán toàn bộ giỏ**, là trường hợp đơn giản. Nếu còn thời gian:
- UI checkbox chọn một số item để thanh toán.
- Transaction chỉ trừ số lượng dòng được chọn; nếu mua `n` trên `cart_qty`, cập nhật `cart_qty = cart_qty - n`; nếu bằng 0 thì xóa dòng.
- Sau cùng recompute `carts.total_quantity`.
- Test partial checkout không được xóa hết các item chưa chọn.

## 6. Tránh lỗi số tiền và tồn kho
- DB `DECIMAL`, Java `BigDecimal`, format ở UI sau khi tính.
- Tồn kho phải kiểm tra **trong transaction với khóa** chứ không chỉ lúc Add to Cart; hàng có thể thay đổi khi chờ checkout.
- Lưu tên/giá sản phẩm vào `order_items` để báo cáo lịch sử không đổi khi ADMIN sửa giá.
- Giá giỏ (`cart_items.unit_price`) **chốt khi thêm giỏ** là quy ước MVP; nếu muốn cập nhật giá mới phải bổ sung bước thông báo/chấp thuận trước xác nhận.

## 7. Class/method
`CheckoutPanel`, `CheckoutRequest`, `CheckoutController.preview/confirm`, `CheckoutService.placeOrder`, `CartDAO.findAndLockItems/clear`, `ProductDAO.lockByIds/decreaseStock`, `OrderDAO.insertOrder/insertOrderItem`, `Order`, `OrderItem`, `InsufficientStockException`.

## 8. Test quan trọng
- [ ] Đặt giỏ 2 loại x 2 và 1, order có đúng 2 `order_items` và tổng tiền.
- [ ] Sau commit, `products.quantity` giảm đúng, Cart rỗng, badge=0.
- [ ] Tên/phone/address rỗng → không tạo order.
- [ ] Giỏ rỗng → không tạo order.
- [ ] Bật mock lỗi ngay sau insert order → rollback; DB không còn order mồ côi.
- [ ] Sau khi mở checkout, ADMIN giảm tồn, USER bấm Confirm → hết hàng, rollback.
- [ ] Bấm nút 2 lần liên tục → chỉ có **1 đơn** cho một lần xác nhận của giỏ (disable nút; transaction đảm bảo).
- [ ] Reload lịch sử: thông tin người nhận, chi tiết và giá snapshot đúng.
- [ ] Test làm tròn/format VNĐ bằng BigDecimal, không xuất hiện sai số.

**Nghiệm thu:** ghi hình checkout thành công + chụp bảng `orders/order_items`, `products.quantity` trước/sau và minh họa một tình huống rollback.
