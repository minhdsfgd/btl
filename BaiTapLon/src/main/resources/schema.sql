-- ============================================================
--  UET Auction System — MySQL Schema
--  Tương ứng với: User, Item, Auction, Bid, Transaction
-- ============================================================

CREATE DATABASE IF NOT EXISTS auction_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auction_db;

-- ── 1. users ─────────────────────────────────────────────────
-- Ánh xạ: User (abstract) → RegularUser / Admin
-- roles lưu dạng chuỗi phân cách bằng dấu phẩy: "BIDDER", "SELLER", "BIDDER,SELLER", "ADMIN"
CREATE TABLE IF NOT EXISTS users (
                                     id         INT           AUTO_INCREMENT PRIMARY KEY,
                                     username   VARCHAR(50)   NOT NULL UNIQUE,
                                     password   VARCHAR(255)  NOT NULL,
                                     balance    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                                     active     TINYINT(1)    NOT NULL DEFAULT 1,
                                     banned     TINYINT(1)    NOT NULL DEFAULT 0,
                                     roles      VARCHAR(100)  NOT NULL,
                                     created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     INDEX idx_username (username)
) ENGINE=InnoDB;

-- ── 2. items ─────────────────────────────────────────────────
-- Ánh xạ: Item (abstract) → Electronics / Art / Vehicle
-- Dùng bảng đơn (Single Table Inheritance) — subclass field nào không dùng để NULL
CREATE TABLE IF NOT EXISTS items (
                                     id              INT           AUTO_INCREMENT PRIMARY KEY,
                                     seller_id       INT           NOT NULL,
                                     name            VARCHAR(200)  NOT NULL,
    description     TEXT,
    starting_price  DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    item_type       ENUM('ELECTRONICS','ART','VEHICLE') NOT NULL,

    -- Electronics
    brand           VARCHAR(100)  DEFAULT '',
    warranty_months INT           DEFAULT 0,

    -- Art
    artist_name     VARCHAR(100)  DEFAULT 'Khuyết danh',
    medium          VARCHAR(100)  DEFAULT '',

    -- Vehicle
    license_plate   VARCHAR(20)   DEFAULT '',
    year_made       INT           DEFAULT 0,

    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_seller (seller_id),
    INDEX idx_type   (item_type)
    ) ENGINE=InnoDB;

-- ── 3. auctions ──────────────────────────────────────────────
-- Ánh xạ: Auction
CREATE TABLE IF NOT EXISTS auctions (
                                        id                 INT           AUTO_INCREMENT PRIMARY KEY,
                                        item_id            INT           NOT NULL,
                                        seller_id          INT           NOT NULL,
                                        current_price      DECIMAL(15,2) NOT NULL,
    bid_increment      DECIMAL(15,2) NOT NULL,
    start_time         DATETIME      NOT NULL,
    end_time           DATETIME      NOT NULL,
    status             ENUM('OPEN','RUNNING','FINISHED','PAID','CANCELED')
    NOT NULL DEFAULT 'OPEN',
    banned             TINYINT(1)    NOT NULL DEFAULT 0,
    leading_bidder_id  INT           DEFAULT NULL,  -- NULL nếu chưa có bid
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (item_id)           REFERENCES items(id)  ON DELETE RESTRICT,
    FOREIGN KEY (seller_id)         REFERENCES users(id)  ON DELETE RESTRICT,
    FOREIGN KEY (leading_bidder_id) REFERENCES users(id)  ON DELETE SET NULL,

    INDEX idx_status    (status),
    INDEX idx_seller_id (seller_id),
    INDEX idx_end_time  (end_time)
    ) ENGINE=InnoDB;

-- ── 4. bids ──────────────────────────────────────────────────
-- Ánh xạ: Bid (immutable)
CREATE TABLE IF NOT EXISTS bids (
                                    id         INT           AUTO_INCREMENT PRIMARY KEY,
                                    auction_id INT           NOT NULL,
                                    user_id    INT           NOT NULL,
                                    amount     DECIMAL(15,2) NOT NULL,
    bid_time   DATETIME      NOT NULL,

    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE RESTRICT,

    INDEX idx_auction (auction_id),
    INDEX idx_user    (user_id)
    ) ENGINE=InnoDB;

-- ── 5. transactions ──────────────────────────────────────────
-- Ánh xạ: Transaction (immutable)
-- from_user_id = NULL khi deposit (không có người gửi)
-- auction_id   = NULL khi không liên quan đến phiên
CREATE TABLE IF NOT EXISTS transactions (
                                            id             INT           AUTO_INCREMENT PRIMARY KEY,
                                            from_user_id   INT           DEFAULT NULL,
                                            to_user_id     INT           NOT NULL,
                                            amount         DECIMAL(15,2) NOT NULL,
    auction_id     INT           DEFAULT NULL,
    type           ENUM('AUCTION_PAYMENT','REFUND','DEPOSIT','ADJUSTMENT') NOT NULL,
    created_at     DATETIME      NOT NULL,

    FOREIGN KEY (from_user_id) REFERENCES users(id)    ON DELETE SET NULL,
    FOREIGN KEY (to_user_id)   REFERENCES users(id)    ON DELETE RESTRICT,
    FOREIGN KEY (auction_id)   REFERENCES auctions(id) ON DELETE SET NULL,

    INDEX idx_from   (from_user_id),
    INDEX idx_to     (to_user_id),
    INDEX idx_auction(auction_id)
    ) ENGINE=InnoDB;

-- ── Seed data: tài khoản Admin mặc định ─────────────────────
-- Password "admin123" — đổi trước khi deploy thật
INSERT IGNORE INTO users (username, password, balance, active, banned, roles)
VALUES ('admin', 'admin123', 0.00, 1, 0, 'ADMIN');