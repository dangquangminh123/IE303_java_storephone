# GIAI ĐOẠN 07 — SEARCH, FILTER, SORT, PAGINATION

**Đích:** lọc nhiều điều kiện kết hợp và chuyển trang đúng tổng số kết quả, áp dụng cả catalog và bảng ADMIN khi cần.  
**Nguồn:** #136–156; bản web dùng Query String, Pageable, JPA Specification, JavaScript.  
**Phụ thuộc:** ProductService, ProductDAO, HomePanel.

## 1. DTO thống nhất
```java
record ProductSearchCriteria(
    String keyword,
    List<String> factories,
    List<String> targets,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String sortKey,
    int page,
    int pageSize
) {}
record PageResult<T>(
    List<T> items, int page, int pageSize,
    long totalItems, int totalPages
) {}
```
Có thể thêm `boolean includeInactive` cho admin nhưng **không nhận cờ này từ màn GUEST**.

## 2. Màn hình
`JTextField txtSearch`, `JComboBox cmbFactory` (hoặc `JCheckBox` nhiều hãng), `JComboBox/checkbox target`, `JTextField minPrice/maxPrice`, `JComboBox sort`, nút Apply/Reset, label `Trang 1 / n`, nút Prev/Next, `JTable`.
- Không truyền filter qua URL; state giữ trong `ProductSearchCriteria`.
- Khi thay keyword/factory/target/price/sort → **reset page=1**.

## 3. Luồng A — Tìm kiếm theo tên
1. GUEST nhập "samsung" → Enter/Search.
2. Controller lấy `keyword.trim()` → `ProductService.search(criteria)`.
3. DAO thêm điều kiện `name LIKE ?` (`"%"+keyword+"%"`) và `active=TRUE`.
4. `PreparedStatement` bind; query `COUNT(*)` và query dữ liệu cùng WHERE.
5. UI render list; không thấy hàng → "Không có sản phẩm phù hợp".

## 4. Luồng B — Lọc nhiều hãng/mục đích/giá
1. User chọn 2 hãng, 1 target, min/max.
2. Service kiểm tra min/max là số không âm, min <= max.
3. DAO tạo SQL động với điều kiện **AND giữa các nhóm**:
   - hãng: `factory IN (?, ?, ...)` (**OR** bên trong nhóm);
   - mục đích: `target IN (?,...)` (**OR** bên trong nhóm);
   - giá: `price >= ?` và/hoặc `price <= ?`;
   - keyword: `name LIKE ?`.
4. Range giá nhiều mức nếu cho phép nhiều lựa chọn: nhóm `(price BETWEEN ? AND ? OR price BETWEEN ? AND ?)`, **không dùng AND** giữa hai khoảng tách rời (bài #150 có ví dụ diễn đạt chưa nhất quán). Ghi rõ đây là hiệu chỉnh logic đề xuất.
5. Trả `PageResult`, gắn tổng số và danh sách.

## 5. Luồng C — Sắp xếp
1. Dropdown: `Mặc định`, `Giá tăng`, `Giá giảm`, `Tên A-Z`, `Mới nhất`.
2. Service chỉ nhận sort key từ **whitelist**, chẳng hạn `PRICE_ASC`, `PRICE_DESC`, `NAME_ASC`, `NEWEST`; map sang câu SQL cố định:
   - `PRICE_ASC → ORDER BY price ASC, id ASC`;
   - `PRICE_DESC → ORDER BY price DESC, id ASC`;
   - `NEWEST → ORDER BY created_at DESC, id DESC`.
3. **Không nhét chuỗi người dùng vào `ORDER BY`**; `PreparedStatement` không bind được tên cột.
4. Khi user đổi sort, page về 1, count và data vẫn thống nhất.

## 6. Luồng D — Phân trang
1. `pageSize=8` hoặc `10` cho demo; page bắt đầu 1.
2. `offset=(page-1)*pageSize` (kiểm tra tràn/âm); query `LIMIT ? OFFSET ?`.
3. `totalPages=ceil(totalItems/pageSize)`, với 0 records có thể hiển thị `Trang 0/0` và disable Prev/Next.
4. Prev khi page >1; Next khi page < totalPages; nếu filter làm tổng page giảm thì clamp page về trang hợp lệ.
5. GUI hiển thị `Tìm thấy N sản phẩm`, `Trang x / y`; mỗi lần next fetch DB thật, không `SELECT *` toàn bộ rồi cắt ở client.

## 7. Luồng E — Giữ trạng thái sau xem detail/cart
Đang ở keyword "Samsung" + hãng Samsung + page=2 → mở chi tiết → đóng dialog → giữ filter, page, sort. Chỉ refresh dữ liệu của trang nếu thay đổi, không reset form. Khi nhấn Reset → clear filter/sort mặc định + page=1.

## 8. Query skeleton
```sql
SELECT id,name,price,quantity,factory,target,image_path
FROM products
WHERE active=TRUE
  AND (? IS NULL OR name LIKE ?)
  AND (? IS NULL OR price >= ?)
  AND (? IS NULL OR price <= ?)
ORDER BY price ASC, id ASC
LIMIT ? OFFSET ?;
```
Đây là minh họa **một phần**; với filter danh sách `IN(...)`, DAO phải sinh chính xác số placeholder và dùng tham số tương ứng. Ưu tiên `StringBuilder where` + `List<Object> params` để dùng lại `WHERE` cho `COUNT`/`SELECT`.

## 9. Test bắt buộc
- [ ] Một filter; nhiều hãng; keyword + hãng + 2 mốc giá; multi-range (nếu dùng).
- [ ] Min > max → báo lỗi.
- [ ] Giá rỗng = không filter; nhập chữ/âm báo lỗi.
- [ ] Next/Prev ở biên bị disable.
- [ ] Tên có ký tự `'`/`%` xử lý đúng và không SQL injection.
- [ ] Chuyển sort không làm rớt filter; trở lại detail không mất state.
- [ ] Tổng số trang chính xác cho 0/1/9/10/11 records khi pageSize=10.
- [ ] Data và COUNT dùng cùng logic WHERE, không báo trang rỗng vô lý.

**Nghiệm thu:** quay video chọn 2 hãng + khoảng giá + giá tăng dần → bấm Next/Prev → mở detail → quay lại vẫn đúng filter.
