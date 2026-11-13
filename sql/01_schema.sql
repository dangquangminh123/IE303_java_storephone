-- ============================================================================
-- STOREPHONE SWING DESKTOP — KHỞI TẠO CƠ SỞ DỮ LIỆU MYSQL 8+
-- Chạy tệp này trong MySQL Workbench (tài khoản có quyền CREATE DATABASE).
-- ============================================================================

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
  VALUES ('ADMIN', 'Quản trị viên'), ('USER', 'Khách hàng')
  ON DUPLICATE KEY UPDATE description = VALUES(description);
