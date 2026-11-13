# GIAI ĐOẠN 06 — TRANG CHỦ, DANH SÁCH VÀ CHI TIẾT ĐIỆN THOẠI

**Đích:** cho GUEST xem catalogue, người đăng nhập xem và thêm từ chi tiết khi đợt 08 hoàn tất.  
**Nguồn:** #72–73, #95–98, #105–106, #134.  
**Phụ thuộc:** sản phẩm mẫu đã có trong database.

## 1. Thiết kế UI
- `HomePanel`: tên cửa hàng, ô tìm kiếm, menu hãng (nếu cần), danh sách điện thoại (JTable hoặc card grid với JPanel), thumbnail, tên, giá VND, tình trạng còn hàng.
- `ProductDetailDialog`: ảnh chính, tên, hãng, mục đích, mô tả ngắn/chi tiết, giá, tồn kho, nút **Thêm vào giỏ** (USER/ADMIN đăng nhập), nút đóng/quay lại.
- Có empty state: "Chưa có sản phẩm phù hợp"; không hiển thị lỗi kỹ thuật SQL cho người mua.

## 2. Luồng A — GUEST mở trang chủ
1. App startup → Navigation `HOME`.
2. `ProductController.loadCatalog(page=1)` → `ProductService.search` với `active=true`.
3. `ProductDAO` query MySQL, mapping ProductDTO.
4. UI đặt các row/card kèm thumbnail, giá; product inactive không xuất hiện.
5. Nếu DB rỗng hiện empty state và nút thử lại; không tạo data giả khi đang chạy thật.

## 3. Luồng B — Xem chi tiết
1. Click/bấm đôi product trên JTable/card.
2. Lấy **productId**, không lấy tên/index để query.
3. `ProductController.openDetail(productId)` → service kiểm tra `active`/tồn tại, trả model/DTO.
4. Dialog hiển thị `ImageIcon`, tên, `MoneyFormatter.format(price)`, factory/target, desc, stock.
5. Nút "Thêm giỏ": GUEST bấm → chuyển Login/thông báo cần login; USER bấm → gọi CartController đợt 08.
6. Nếu sản phẩm bị ẩn hoặc hết tồn: nút thêm giỏ disable; detail vẫn có thể báo hết hàng nếu được phép hiển thị.

## 4. Luồng C — Quay lại / đồng bộ dữ liệu
1. Đóng detail → trở về đúng trạng thái lọc/trang hiện tại.
2. Sau khi ADMIN sửa product, khi quay lại Home phải reload dữ liệu từ DAO.
3. Giao diện không tạo thêm JFrame; chỉ dialog + CardLayout.
4. Load ảnh ngoài EDT nếu ảnh lớn, scale thành thumbnail; không chặn thao tác người dùng khi query DB.

## 5. Class/method
`HomePanel`, `ProductDetailDialog`, `ProductController.openDetail/reloadCatalog`, `ProductService.findActiveById/search`, `ProductDAO.findById/search`, `ProductCard` (nếu dùng grid), `ProductTableModel`, `MoneyFormatter`, `ImageStorage`.

## 6. Bài test
- [ ] GUEST xem được list/detail không cần login.
- [ ] GUEST không checkout được.
- [ ] Product inactive không hiển thị công khai.
- [ ] Giá/tồn/detail/ảnh lấy đúng DB.
- [ ] Product chưa có ảnh dùng placeholder an toàn.
- [ ] Đóng detail không reset bộ lọc (nếu áp dụng sau ở 07).
- [ ] Double click row sau khi sort vẫn mở **đúng product ID**.
- [ ] Tắt/mở app dữ liệu vẫn giữ.

**Nghiệm thu:** ghi video demo GUEST vào home → chọn điện thoại → xem ảnh, giá, mô tả → bấm add cart thì yêu cầu login.
