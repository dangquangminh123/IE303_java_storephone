# GIAI ĐOẠN 10 — QUẢN LÝ ĐƠN HÀNG ADMIN + LỊCH SỬ NGƯỜI MUA

**Đích:** ADMIN xem mọi đơn, mở chi tiết, chuyển trạng thái; USER chỉ xem đơn của mình.  
**Nguồn:** #132–134; bài #132 ghi admin **không tự tạo đơn**.  
**Phụ thuộc:** checkout thành công và DB có orders.

## 1. Màn hình
- `AdminOrderPanel`: JTable `Mã đơn, Khách mua, Người nhận, Tổng tiền, Trạng thái, Ngày tạo`; filter status/date; `Xem chi tiết`, `Cập nhật trạng thái`.
- `OrderDetailDialog`: danh sách `order_items` với giá snapshot, địa chỉ/phone nhận, tổng, lịch sử xử lý nếu sau này mở rộng.
- `OrderHistoryPanel` (USER): chỉ đơn của chính mình, xem detail và trạng thái; không có nút duyệt trạng thái.
- `OrderTableModel` là mẫu hiển thị; phần query/lọc status phải thực hiện từ DAO.

## 2. Luồng A — ADMIN xem danh sách đơn
1. ADMIN bấm Quản lý đơn → `OrderController.loadAll()`.
2. `OrderService.listAll(page,filters)` gọi `requireAdmin()` rồi `OrderDAO.findAll`.
3. Render JTable, có thể phân trang nếu nhiều đơn.
4. Chọn dòng → lấy `orderId` đúng từ model, query DB mới nhất.
5. `OrderDAO.findItemsByOrderId` → mở dialog chi tiết Order + OrderItems.
6. **Không có nút “Tạo đơn trong admin”**, đúng bài #132.

## 3. Luồng B — ADMIN cập nhật trạng thái
**Nguồn chỉ nói đổi trạng thái mà không chốt quy trình; các bước sau là đề xuất rõ ràng để bài cuối kỳ nhất quán.**
```text
PENDING → CONFIRMED → SHIPPING → COMPLETED
    \→ CANCELLED   \→ CANCELLED (trước khi giao)
```
Quy tắc đơn giản:
- `PENDING` được xác nhận hoặc hủy.
- `CONFIRMED` được chuyển `SHIPPING` hoặc hủy.
- `SHIPPING` chuyển `COMPLETED`; để đơn giản, **không cho hủy sau SHIPPING**.
- `COMPLETED` và `CANCELLED` là trạng thái cuối, không chuyển tiếp.
- Khi hủy `PENDING`/`CONFIRMED`, **hoàn lại kho đúng một lần** trong transaction. Trong phiên bản không hỗ trợ hủy sau shipping, không cần nghiệp vụ hoàn phức tạp.

Các bước:
1. ADMIN chọn order, click cập nhật → ComboBox chỉ hiển thị trạng thái hợp lệ.
2. Service `requireAdmin`, mở transaction, lock order `SELECT ... FOR UPDATE`.
3. Kiểm tra trạng thái DB hiện tại (không tin trạng thái JTable cũ).
4. Nếu chuyển sang CANCELLED: đọc `order_items`, hoàn quantity product, đổi status **trong cùng transaction**. Nếu đã CANCELLED thì từ chối, không hoàn lần hai.
5. Nếu chuyển trạng thái bình thường: `UPDATE orders SET status=? WHERE id=?`.
6. Commit; reload admin list và history ở lần người mua mở. Lỗi → rollback.

## 4. Luồng C — USER xem lịch sử của mình
1. USER bấm Lịch sử → Controller lấy `currentUserId` từ AppSession.
2. `OrderService.findMine(page)` → DAO query `WHERE user_id=? ORDER BY created_at DESC`.
3. UI hiển thị mã, tổng, status, ngày; chọn `Xem chi tiết`.
4. Service `getDetail(orderId)` kiểm tra `order.userId == currentUserId` hoặc ADMIN; **không thể đọc đơn người khác** kể cả nhập ID bất kỳ.
5. Dialog read-only, giá lấy từ `order_items.unit_price`, không từ bảng Products.
6. Có thể thêm thông báo status khi mở lại; không có push realtime trong Swing thuần.

## 5. Class/method
`AdminOrderPanel`, `OrderHistoryPanel`, `OrderDetailDialog`, `OrderController`, `OrderService.listAll/findMine/getDetail/changeStatus`, `OrderDAO.findAll/findByUserId/findById/findItems/updateStatus`, `ProductDAO.restoreStock`.

## 6. Test nghiệm thu
- [ ] ADMIN thấy mọi đơn; USER A chỉ thấy đơn A, USER B chỉ thấy đơn B.
- [ ] USER A gọi xem ID đơn B → bị từ chối ở service.
- [ ] ADMIN chọn order → xem đúng các dòng chi tiết.
- [ ] Khi update trạng thái hợp lệ, UI/history phản ánh đúng.
- [ ] Chuyển `COMPLETED → PENDING` hoặc `SHIPPING → CANCELLED` bị chặn theo quy tắc đã chốt.
- [ ] Hủy order đã trừ 3 sản phẩm → tồn tăng lại 3 đúng một lần.
- [ ] Bấm hủy hai lần → không hoàn kho lần 2.
- [ ] Đơn cũ không thay đổi tổng nếu Product bị đổi giá/ẩn.
- [ ] USER không có đơn → hiện thông báo phù hợp.

**Nghiệm thu:** USER đặt đơn → ADMIN đổi PENDING→CONFIRMED→SHIPPING→COMPLETED → USER vào lịch sử thấy COMPLETED, xem order items/giá chính xác; thêm demo hủy một đơn khác và hoàn kho.
