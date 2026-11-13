# THIẾT KẾ DATABASE MYSQL — STOREPHONE SWING

## 1. Quy ước thiết kế
Dựa vào các model trong tài liệu Hỏi Dân IT `User/Role/Product/Cart/CartDetail/Order/OrderDetail` (đặc biệt #74–81, #124, #130–132). Các cột trạng thái tồn kho, giá snapshot và thời gian được **bổ sung có chủ đích** để làm desktop an toàn và dễ báo cáo.

- Dùng MySQL **8.0.16 trở lên** để `CHECK` constraint có hiệu lực.
- Tên bảng số nhiều để tránh từ khóa SQL `ORDER`, `USER`.
- PK `BIGINT AUTO_INCREMENT`, FK đầy đủ.
- Tiền VND: `DECIMAL(15,0)` trên DB, `BigDecimal` trong Java; **không dùng double**.
- Không lưu mật khẩu gốc. `users.password_hash` chỉ nhận BCrypt.
- Ảnh dùng `image_path` là **đường dẫn tương đối**, không chứa bytes ảnh hay địa chỉ tuyệt đối của máy lập trình.
- Nên thống nhất database `storephone_db`, charset `utf8mb4`, timezone theo máy demo.

## 2. ERD
```mermaid
erDiagram
    ROLES ||--o{ USERS : role_id
    USERS ||--o| CARTS : user_id
    CARTS ||--o{ CART_ITEMS : cart_id
    PRODUCTS ||--o{ CART_ITEMS : product_id
    USERS ||--o{ ORDERS : user_id
    ORDERS ||--|{ ORDER_ITEMS : order_id
    PRODUCTS ||--o{ ORDER_ITEMS : product_id

    ROLES { bigint id PK; varchar name UK }
    USERS { bigint id PK; bigint role_id FK; varchar email UK; varchar password_hash; varchar full_name; boolean active }
    PRODUCTS { bigint id PK; varchar name; decimal price; int quantity; varchar factory; varchar target; varchar image_path }
    CARTS { bigint id PK; bigint user_id FK_UK; int total_quantity }
    CART_ITEMS { bigint id PK; bigint cart_id FK; bigint product_id FK; int quantity; decimal unit_price }
    ORDERS { bigint id PK; bigint user_id FK; varchar status; decimal total_amount; varchar receiver_name }
    ORDER_ITEMS { bigint id PK; bigint order_id FK; bigint product_id FK; int quantity; decimal unit_price }
```

## 3. Script khởi tạo — chạy trong MySQL Workbench
```sql
CREATE DATABASE IF NOT EXISTS storephone_db
 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE storephone_db;

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(20) NOT NULL UNIQUE,
  description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(150) NOT NULL,
  phone VARCHAR(30),
  address VARCHAR(255),
  avatar_path VARCHAR(255),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(180) NOT NULL,
  price DECIMAL(15,0) NOT NULL,
  short_desc VARCHAR(500),
  detail_desc MEDIUMTEXT,
  quantity INT NOT NULL DEFAULT 0,
  factory VARCHAR(80) NOT NULL,
  target VARCHAR(100),
  image_path VARCHAR(255),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT ck_product_price CHECK (price >= 0),
  CONSTRAINT ck_product_stock CHECK (quantity >= 0),
  INDEX idx_product_factory (factory),
  INDEX idx_product_price (price),
  INDEX idx_product_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS carts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  total_quantity INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_cart_qty CHECK (total_quantity >= 0),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(15,0) NOT NULL,
  CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id),
  CONSTRAINT ck_cartitem_qty CHECK (quantity > 0),
  FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  receiver_name VARCHAR(150) NOT NULL,
  receiver_phone VARCHAR(30) NOT NULL,
  receiver_address VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  total_amount DECIMAL(15,0) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_order_status CHECK (status IN
    ('PENDING','CONFIRMED','SHIPPING','COMPLETED','CANCELLED')),
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_order_user_date (user_id, created_at),
  INDEX idx_order_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(180) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(15,0) NOT NULL,
  CONSTRAINT ck_orderitem_qty CHECK (quantity > 0),
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

INSERT INTO roles(name, description)
 VALUES ('ADMIN','Quản trị viên'), ('USER','Khách hàng')
 ON DUPLICATE KEY UPDATE description=VALUES(description);
```

**Lưu ý:** script không tạo admin với mật khẩu plaintext. Sau khi chạy schema, tạo admin bằng công cụ `SeedAdmin` một lần, **hash BCrypt trước khi INSERT**. Không commit file cấu hình chứa mật khẩu MySQL/seed admin. Demo với tài khoản mẫu vô hại, đổi mật khẩu sau khi seed.

## 4. Sơ đồ quan hệ và cách lấy dữ liệu
| Thực thể | Quan hệ | DAO cần có |
|---|---|---|
| Role → Users | 1:N | `UserDAO.findByRole()` |
| User → Cart | 1:0..1 | `CartDAO.findByUserId()` |
| Cart → CartItem | 1:N | `CartDAO.findItemsByCartId()` |
| Product → CartItem | 1:N | `CartDAO.upsertItem()` |
| User → Orders | 1:N | `OrderDAO.findByUserId()` |
| Order → OrderItem | 1:N | `OrderDAO.findItems(orderId)` |
| Product → OrderItem | 1:N | đọc giá/snapshot cũ |

## 5. Ràng buộc nhất quán
- `users.email` duy nhất, email chuẩn hóa `.trim().toLowerCase()` trước khi so.
- `products.quantity` không âm, `cart_items.quantity > 0`.
- Mỗi user có tối đa 1 cart; mỗi cart có tối đa 1 cart item trên mỗi product.
- Một order có ít nhất 1 order item (kiểm tra bằng service trong cùng transaction; DB FK không tự đảm bảo order không rỗng).
- Chỉ `ACTIVE product` và đủ stock mới cho đặt hàng.
- `order_items.unit_price` và `product_name` là snapshot để lịch sử giữ nguyên khi sửa product.
- Không xóa cứng user/product đã được tham chiếu; dùng `active=false`.
- `carts.total_quantity = SUM(cart_items.quantity)`. Tránh lỗi tài liệu #129 khi xóa dòng có quantity >1 mà chỉ trừ 1.

## 6. API query cơ bản dùng `PreparedStatement`
```sql
-- Chi tiết giỏ
SELECT ci.id, ci.product_id, p.name, ci.quantity, ci.unit_price
FROM cart_items ci JOIN products p ON p.id=ci.product_id
WHERE ci.cart_id = ?;

-- Cập nhật số lượng tổng giỏ (thực hiện cùng transaction)
UPDATE carts SET total_quantity =
  (SELECT COUNTED.qty FROM (
    SELECT COALESCE(SUM(quantity),0) AS qty
    FROM cart_items WHERE cart_id=?
  ) AS COUNTED)
WHERE id=?;

-- Khóa hàng khi checkout
SELECT id, name, quantity, price, active
FROM products WHERE id=? FOR UPDATE;

-- Chặn âm kho (điều kiện bổ sung)
UPDATE products SET quantity=quantity-?
WHERE id=? AND active=TRUE AND quantity>=?;

-- Tìm/lọc/đếm/pagination (điều kiện WHERE giống nhau)
SELECT id, name, price, factory, quantity
FROM products WHERE active=TRUE AND name LIKE ?
ORDER BY price ASC, id ASC LIMIT ? OFFSET ?;
SELECT COUNT(*) FROM products WHERE active=TRUE AND name LIKE ?;
```

Câu `UPDATE carts` có thể đơn giản hóa thành `UPDATE carts SET total_quantity=? WHERE id=?` sau khi tính tổng bằng `SELECT COALESCE(SUM(...),0)` trong transaction. Chọn **một** triển khai để dễ debug.

## 7. Transaction quan trọng
Duy nhất **một** `Connection` được truyền qua các DAO làm một lần checkout, không để mỗi DAO tự mở connection mới:
```text
connection.setAutoCommit(false)
  → lock các product theo id tăng dần (FOR UPDATE)
  → kiểm tra đủ tồn + giá
  → INSERT orders
  → INSERT order_items
  → UPDATE products trừ tồn
  → DELETE cart_items theo cart
  → UPDATE/DELETE cart
  → COMMIT
nếu lỗi → ROLLBACK → thông báo cho UI
finally → đóng connection
```
Ở đơn hủy, chỉ hoàn kho nếu trạng thái trước đó chưa hủy và chính sách đã chốt cho hoàn kho; việc hoàn kho phải chạy **cùng transaction cập nhật trạng thái**, tránh hoàn hai lần. Trong báo cáo cần nói rõ quy tắc này là bổ sung so với tài liệu học.

## 8. Dữ liệu test
- 2 role: `ADMIN`, `USER`.
- 1 admin, 2 khách test (đăng ký qua app).
- 12–20 điện thoại giả lập có tên, hãng, `target`, giá và quantity đa dạng; ít nhất 1 sản phẩm `quantity=0`.
- Test các đơn `PENDING`, `COMPLETED`, `CANCELLED`.
- Đảm bảo ảnh minh họa có quyền sử dụng và không phụ thuộc đường dẫn máy người code.

## 9. Kiểm tra SQL xong trước khi viết UI
```sql
SHOW TABLES;
SELECT * FROM roles;
DESCRIBE products;
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM products;
```
Bài test: chèn cart item trùng `(cart_id, product_id)` phải bị chặn; quantity âm phải bị từ chối; xoá product đã có order phải bị FK chặn (app chọn ẩn/deactivate).
