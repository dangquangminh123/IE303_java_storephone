# GIAI ĐOẠN 11 — DASHBOARD THỐNG KÊ CHO ADMIN

**Đích:** ADMIN nhìn thấy số liệu tổng quan tính từ MySQL.  
**Nguồn:** #133 chỉ ghi “Xây dựng dashboard”, **không mô tả chi tiết chỉ số/công thức**. Phần dưới là **đề xuất bổ sung phục vụ báo cáo cuối kỳ**.  
**Phụ thuộc:** `users/products/orders/order_items` có dữ liệu từ các đợt trước.

## 1. Giao diện
`AdminDashboardPanel`: 4 KPI card, bảng 5 đơn gần đây, danh sách 5 điện thoại bán chạy (nếu có); tùy thời gian có thể vẽ chart bằng Java 2D/JFreeChart, nhưng **không bắt buộc**.

KPI đề xuất:
1. **Tổng số tài khoản đang hoạt động**: `COUNT(*) users WHERE active=true` (có thể lọc role USER).
2. **Sản phẩm đang kinh doanh**: `COUNT(*) products WHERE active=true`.
3. **Đơn chờ xử lý**: `COUNT(*) orders WHERE status='PENDING'`.
4. **Doanh thu hoàn tất**: `SUM(total_amount) orders WHERE status='COMPLETED'`.

## 2. Luồng A — Mở dashboard
1. ADMIN login → `Navigation.show(ADMIN_DASHBOARD)`.
2. `DashboardController.reload()` chạy `DashboardService.getSummary` trong `SwingWorker`.
3. `AuthorizationService.requireAdmin`.
4. DAO thực hiện các query aggregate từ DB, không loop qua toàn bộ order rồi tính tại Swing.
5. Trả `DashboardSummary` DTO → update KPI labels, JTable recent orders.
6. Nếu chưa có dữ liệu: zero thay vì null; định dạng VNĐ rõ.
7. Nút Refresh cho admin lấy lại số liệu mới nhất từ DB.

## 3. Luồng B — Xem đơn gần đây
1. `SELECT id, status, total_amount, created_at FROM orders ORDER BY created_at DESC LIMIT 5;`
2. Click một dòng → điều hướng `AdminOrderPanel` và mở đúng orderId (nếu đã hỗ trợ action).
3. Nếu order bị cập nhật trong màn quản lý, quay về Dashboard → refresh tự động hoặc bấm Refresh.

## 4. Luồng C — Top sản phẩm (không bắt buộc)
```sql
SELECT oi.product_id, oi.product_name, SUM(oi.quantity) AS sold
FROM order_items oi JOIN orders o ON o.id=oi.order_id
WHERE o.status='COMPLETED'
GROUP BY oi.product_id, oi.product_name
ORDER BY sold DESC LIMIT 5;
```
Những đơn `CANCELLED` hoặc chưa `COMPLETED` không được tính vào doanh thu/top sales; đây là **quy ước KPI đề xuất**, phải nói rõ khi thuyết trình.

## 5. Class/method
`AdminDashboardPanel`, `DashboardController.reload`, `DashboardService.getSummary`, `DashboardDAO.getCounts/getRecentOrders/getTopProducts`, `DashboardSummary`, `MoneyFormatter`.

## 6. Test nghiệm thu
- [ ] USER/GUEST không mở dashboard.
- [ ] Không có order → doanh thu 0, không null pointer.
- [ ] Có PENDING 2 đơn → KPI pending=2.
- [ ] Đổi PENDING→COMPLETED → pending giảm, revenue tăng đúng.
- [ ] Hủy đơn → doanh thu không tính đơn hủy.
- [ ] Refresh phản ánh dữ liệu Workbench, không cần restart app.
- [ ] Thống kê VND không lỗi dấu phân cách.
- [ ] Bảng recent orders sắp theo ngày mới nhất.

**Nghiệm thu:** demo trước/sau khi ADMIN xác nhận hoàn tất một đơn và so sánh số liệu KPI từ SQL.
