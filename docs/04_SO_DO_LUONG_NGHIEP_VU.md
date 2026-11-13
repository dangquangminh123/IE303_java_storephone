# SƠ ĐỒ LUỒNG NGHIỆP VỤ — DÙNG CHO CODE VÀ BÁO CÁO

> Những sơ đồ này mô tả **cách người dùng sử dụng phần mềm** và lớp xử lý tương ứng. Nguồn chức năng: tài liệu Spring MVC các bài #51–156; kỹ thuật Swing/JDBC là thiết kế chuyển đổi.

## 1. Luồng người dùng tổng quan
```mermaid
flowchart TD
  Start[Mở ứng dụng Swing] --> Catalog[Danh sách điện thoại - GUEST]
  Catalog --> Detail[Xem chi tiết]
  Catalog --> Search[Tìm / lọc / sắp xếp / phân trang]
  Search --> Detail
  Detail --> Add{Đã đăng nhập?}
  Add -- Không --> Login[Đăng nhập / Đăng ký]
  Add -- Có --> Cart[Thêm vào giỏ]
  Login --> Role{Vai trò}
  Role -- USER --> Cart
  Role -- ADMIN --> Admin[Dashboard / Quản lý]
  Cart --> Checkout[Nhập thông tin nhận hàng]
  Checkout --> Confirm[Đặt hàng]
  Confirm --> History[Lịch sử đơn hàng]
  Admin --> Product[CRUD điện thoại]
  Admin --> User[CRUD người dùng]
  Admin --> Order[Quản lý đơn]
```

## 2. Luồng login và role
```mermaid
flowchart TD
  A[Nhập email + mật khẩu] --> B[AuthController]
  B --> C[AuthService.validate]
  C --> D[UserDAO.findByEmail]
  D --> E{Tồn tại và active?}
  E -- Không --> Err[Thông báo sai thông tin]
  E -- Có --> F{BCrypt khớp?}
  F -- Không --> Err
  F -- Có --> G[AppSession.setCurrentUser]
  G --> H{Role}
  H -- ADMIN --> I[Admin Dashboard]
  H -- USER --> J[Catalog + Cart]
```

## 3. Luồng admin CRUD user và product
```mermaid
flowchart TD
  A[Admin mở màn hình danh sách] --> B[DAO truy vấn page]
  B --> C[JTable hiển thị]
  C --> D{Thao tác}
  D -- Thêm --> E[JDialog tạo]
  D -- Sửa --> F[JDialog nạp giá trị]
  D -- Xem --> G[JDialog chi tiết]
  D -- Xóa --> H[Xác nhận / deactivate]
  E --> V[Validator + role check]
  F --> V
  H --> V
  V --> S[Service → JDBC → MySQL]
  S --> C
```

## 4. Luồng giỏ hàng
```mermaid
flowchart TD
  A[USER chọn Thêm vào giỏ] --> B[CartService]
  B --> C{Có Cart?}
  C -- Chưa --> D[Tạo Cart]
  C -- Có --> E[Đọc CartItem]
  D --> E
  E --> F{Sản phẩm đã có?}
  F -- Có --> G[Tăng quantity]
  F -- Chưa --> H[Insert CartItem]
  G --> I[Recompute total_quantity]
  H --> I
  I --> J[Commit / refresh CartPanel]
  J --> K{USER tiếp tục}
  K -- Tăng/giảm --> L[Update quantity + validate]
  K -- Xóa --> M[Delete CartItem + update total]
  L --> J
  M --> J
```

## 5. Luồng checkout có transaction
```mermaid
flowchart TD
  A[USER mở Checkout] --> B[Hiển thị giỏ + tổng tiền]
  B --> C[Nhập người nhận / SĐT / địa chỉ]
  C --> D[Nhấn Đặt hàng]
  D --> E[Validate session + cart + thông tin]
  E --> F[BEGIN transaction]
  F --> G[Lock product & kiểm tra tồn kho]
  G --> H{Đủ hàng?}
  H -- Không --> R[ROLLBACK + báo lỗi]
  H -- Có --> I[Insert orders + order_items]
  I --> J[Trừ tồn kho]
  J --> K[Xóa cart_items / đặt tổng giỏ về 0]
  K --> L[COMMIT]
  L --> M[Thông báo thành công + mở lịch sử]
```

## 6. Luồng admin xử lý đơn và người mua xem lịch sử
```mermaid
flowchart LR
  U[USER mở lịch sử] --> UH[OrderService.findMine]
  UH --> UD[JTable chỉ đơn của USER]
  A[ADMIN mở quản lý đơn] --> AL[OrderService.listAll]
  AL --> AD[JTable + chi tiết order_items]
  AD --> ST{Đổi trạng thái hợp lệ?}
  ST -- Có --> SAVE[Transaction: update status / hoàn kho nếu hủy]
  ST -- Không --> ERR[Báo không được chuyển]
  SAVE --> AL
```

## 7. Luồng filter + pagination đồng nhất
```mermaid
flowchart TD
  A[Nhập keyword, chọn hãng, giá, mục đích, sort] --> B[ProductSearchCriteria]
  B --> C[Page=1 khi đổi filter]
  C --> D[ProductService.search]
  D --> E[ProductDAO - build WHERE + args]
  E --> F[SELECT ... LIMIT OFFSET]
  E --> G[COUNT(*) cùng điều kiện]
  F --> H[PageResult]
  G --> H
  H --> I[Update JTable + page x / n]
  I --> J[Next / Prev → lại Service]
```

## 8. Bản đồ màn hình
| Nhóm | Màn hình | Ai thấy | Hành động |
|---|---|---|---|
| Chung | `HomePanel` | GUEST/USER/ADMIN | Browse, search, xem detail |
| Chung | `ProductDetailDialog` | GUEST/USER/ADMIN | Xem mô tả, ảnh, giá, tồn; USER add cart |
| Auth | `LoginPanel`, `RegisterPanel` | Chưa login | Login/register |
| Người mua | `CartPanel` | USER/ADMIN | Xem, tăng/giảm/xóa |
| Người mua | `CheckoutPanel` | USER/ADMIN | Nhập người nhận, đặt đơn |
| Người mua | `OrderHistoryPanel` | USER/ADMIN | Xem các đơn của chính mình |
| Quản trị | `AdminDashboardPanel` | ADMIN | Tổng quan |
| Quản trị | `AdminUserPanel` | ADMIN | CRUD user |
| Quản trị | `AdminProductPanel` | ADMIN | CRUD product + ảnh |
| Quản trị | `AdminOrderPanel` | ADMIN | Xem/đổi trạng thái đơn |

## 9. Cách kể luồng khi thuyết trình
1. Người dùng tác động trên **Swing View**.
2. Controller chuyển yêu cầu thành DTO.
3. Service kiểm tra đăng nhập, quyền, điều kiện nghiệp vụ.
4. DAO dùng JDBC thao tác MySQL.
5. Service trả result hoặc lỗi.
6. Swing cập nhật `JTable`/`JPanel` trên EDT.
7. Riêng checkout, nhiều thao tác DB liên quan phải nằm **cùng một transaction**.

Không nói “trình duyệt gọi API” khi dự án **không có web backend**. Sơ đồ không có HTTP route; tất cả hành động là sự kiện của UI desktop.
