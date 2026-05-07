DROP DATABASE IF EXISTS auction_db;
CREATE DATABASE auction_db;

USE auction_db;

-- =========================
-- USERS
-- =========================

CREATE TABLE users (
                       user_id INT PRIMARY KEY AUTO_INCREMENT,

                       username VARCHAR(50) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,

                       full_name VARCHAR(100),

                       role ENUM('ADMIN', 'USER') DEFAULT 'USER',

                       balance DECIMAL(15,2) DEFAULT 0,

                       banned BOOLEAN DEFAULT FALSE,

                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- ITEMS
-- =========================

CREATE TABLE items (
                       item_id INT PRIMARY KEY AUTO_INCREMENT,

                       seller_id INT NOT NULL,

                       title VARCHAR(255) NOT NULL,

                       description TEXT,

                       category ENUM(
        'ART',
        'ELECTRONICS',
        'VEHICLE'
    ),

                       starting_price DECIMAL(15,2) NOT NULL,

                       image_url TEXT,

                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                       FOREIGN KEY (seller_id)
                           REFERENCES users(user_id)
);

-- =========================
-- AUCTIONS
-- =========================

CREATE TABLE auctions (
                          auction_id INT PRIMARY KEY AUTO_INCREMENT,

                          item_id INT NOT NULL,

                          seller_id INT NOT NULL,

                          current_price DECIMAL(15,2) NOT NULL,

                          bid_increment DECIMAL(15,2) DEFAULT 10,

                          start_time DATETIME,

                          end_time DATETIME,

                          status ENUM(
        'OPEN',
        'RUNNING',
        'FINISHED',
        'PAID',
        'CANCELED'
    ),

                          leading_bidder_id INT,

                          FOREIGN KEY (item_id)
                              REFERENCES items(item_id),

                          FOREIGN KEY (seller_id)
                              REFERENCES users(user_id),

                          FOREIGN KEY (leading_bidder_id)
                              REFERENCES users(user_id)
);

-- =========================
-- BIDS
-- =========================

CREATE TABLE bids (
                      bid_id INT PRIMARY KEY AUTO_INCREMENT,

                      auction_id INT NOT NULL,

                      user_id INT NOT NULL,

                      amount DECIMAL(15,2) NOT NULL,

                      bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                      FOREIGN KEY (auction_id)
                          REFERENCES auctions(auction_id),

                      FOREIGN KEY (user_id)
                          REFERENCES users(user_id)
);

-- =========================
-- TRANSACTIONS
-- =========================

CREATE TABLE transactions (
                              transaction_id INT PRIMARY KEY AUTO_INCREMENT,

                              auction_id INT NOT NULL,

                              from_user_id INT NOT NULL,

                              to_user_id INT NOT NULL,

                              amount DECIMAL(15,2) NOT NULL,

                              transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                              FOREIGN KEY (auction_id)
                                  REFERENCES auctions(auction_id),

                              FOREIGN KEY (from_user_id)
                                  REFERENCES users(user_id),

                              FOREIGN KEY (to_user_id)
                                  REFERENCES users(user_id)
);