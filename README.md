# UET Auction System

## 📋 Mô tả hệ thống

**UET Auction System** là một hệ thống đấu giá trực tuyến được xây dựng theo kiến trúc **Client-Server** sử dụng **Java Socket**. Hệ thống cho phép người dùng đấu giá, quản lý sản phẩm, và theo dõi các giao dịch trong thời gian thực.

### Phạm vi hệ thống
- **Người dùng bình thường**: Đăng ký, đăng nhập, theo dõi đấu giá, đấu giá, xem lịch sử giao dịch
- **Admin**: Quản lý người dùng, cấp role, cấm người dùng, quản lý mặt hàng, theo dõi audit log
- **Tính năng chính**: Xác thực người dùng, hệ thống đấu giá tự động, quản lý ví (balance), ghi log hoạt động

---

## 🔧 Công nghệ sử dụng

| Thành phần | Công nghệ |
|-----------|-----------|
| **Ngôn ngữ** | Java 21+ |
| **Frontend** | JavaFX 25 (GUI Desktop) |
| **Backend** | Java Socket (Server TCP) |
| **Database** | MySQL 8.3+ |
| **Build Tool** | Maven 3.9+ |
| **Testing** | JUnit 5, Mockito |
| **Bảo mật** | BCrypt (Hash password) |
| **Mã hóa** | Base64 |

### Yêu cầu cài đặt

**Hệ điều hành hỗ trợ**: Windows, macOS, Linux

**Phần mềm cần thiết**:
- **Java Development Kit (JDK) 21 hoặc cao hơn**
  - Tải: https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html
  - Hoặc: https://adoptium.net/ (Temurin OpenJDK)

- **MySQL Server 8.0+**
  - Tải: https://dev.mysql.com/downloads/mysql/
  - Hoặc dùng Docker: `docker run -d -e MYSQL_ROOT_PASSWORD=password -p 3306:3306 mysql:8`

- **Maven 3.9+** (nếu chưa có)
  - Tải: https://maven.apache.org/download.cgi
  - Kiểm tra: `mvn --version`

- **Git** (để clone repo)
  - Tải: https://git-scm.com/

---

## 📁 Cấu trúc thư mục

```
BaiTapLon/
├── src/
│   ├── main/
│   │   ├── java/com/code/
│   │   │   ├── models/              # Entity: User, Item, Auction, Bid, Transaction, Role, AuditLog
│   │   │   ├── dao/                 # Data Access Object: UserDAO, ItemDAO, AuctionDAO, BidDAO, TransactionDAO, AuditLogDAO
│   │   │   ├── service/             # Business Logic: UserService, ItemService, AuctionService, BidService, TransactionService
│   │   │   ├── server/              # Server: AuctionServer, ClientHandler, RequestProcessor, DataSeeder
│   │   │   ├── client/              # Client: SocketClient, AppConfig
│   │   │   ├── network/             # Giao thức: Request, Response, RequestType
│   │   │   ├── database/            # Kết nối: DBConnection
│   │   │   ├── util/                # Tiện ích: PasswordHasher, DateUtil, ViewUtil
│   │   │   ├── exception/           # Exception tùy chỉnh
│   │   │   ├── views/               # FXML (UI layout)
│   │   │   ├── controllers/         # FXML Controller (xử lý sự kiện UI)
│   │   │   ├── ClientApp.java       # Entry point Client
│   │   │   └── AuctionServer.java   # Entry point Server
│   │   ├── resources/
│   │   │   ├── db.properties        # Cấu hình database
│   │   │   ├── config.properties    # Cấu hình server (IP, port)
│   │   │   └── com/code/views/      # FXML files (UI definition)
│   └── test/
│       └── java/com/code/           # Unit tests
├── pom.xml                          # Maven configuration
└── README.md
```

### Module chính

1. **Models** (`models/`): Định nghĩa các entity
   - `User`, `Item`, `Auction`, `Bid`, `Transaction`, `AuditLog`

2. **DAO** (`dao/`): Truy vấn database trực tiếp
   - Sử dụng JDBC

3. **Service** (`service/`): Xử lý logic nghiệp vụ
   - Kiểm tra điều kiện, tính toán, gọi DAO

4. **Server** (`server/`):
   - `AuctionServer`: Chạy server, lắng nghe client
   - `ClientHandler`: Xử lý mỗi client trong thread riêng
   - `RequestProcessor`: Định tuyến request → Service
   - `DataSeeder`: Tạo dữ liệu mẫu khi server chạy lần đầu

5. **Client** (`client/`):
   - `SocketClient`: Kết nối đến server
   - `SocketClientListener`: Nhận message từ server (background thread)

6. **Network** (`network/`):
   - `Request`: Yêu cầu từ client → server
   - `Response`: Trả lời từ server → client
   - `RequestType`: Enum định danh loại request

---

## 🚀 Hướng dẫn chạy

### Bước 1: Clone repository

```bash
git clone https://github.com/minhdsfgd/btl.git
cd btl/BaiTapLon
```

### Bước 2: Cấu hình Database

#### 2.1 Tạo database (MySQL)

Mở MySQL shell hoặc MySQL Workbench, chạy script SQL:

```sql
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auction_db;
-- Các table sẽ được tạo tự động khi chạy server lần đầu
```

#### 2.2 Cập nhật file cấu hình database

**File**: `src/main/resources/db.properties`

```properties
db.url=jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
db.username=root
db.password=MAT_KHAU_CUA_BAN
db.pool66.size=10
```

**Thay thế**:
- `MAT_KHAU_CUA_BAN` → mật khẩu MySQL của bạn
- `localhost:3306` → nếu MySQL chạy trên máy khác

### Bước 3: Cập nhật file cấu hình Client (tuỳ chọn)

**File**: `src/main/resources/config.properties`

```properties
server.host=localhost
server.port=8888
```

**Nếu server chạy trên máy khác**, thay `localhost` bằng IP server.

### Bước 4: Biên dịch dự án

#### **Windows**:
```bash
mvn clean install
```

#### **macOS / Linux**:
```bash
mvn clean install
```

Lệnh này sẽ:
- Tải các dependency từ Maven Repository
- Biên dịch source code
- Chạy unit test
- Tạo JAR file

### Bước 5: Chạy Server và Client

#### **5.1 Chạy Server** (terminal/cmd 1)

**Cách 1: Chạy từ IDE (IntelliJ IDEA / Eclipse)**
- Mở file `src/main/java/com/code/server/AuctionServer.java`
- Click `Run` → `Run 'AuctionServer.main()'`

**Cách 2: Chạy từ command line**

**Windows**:
```bash
mvn clean compile exec:java@run-server
```

**macOS / Linux**:
```bash
mvn clean compile exec:java@run-server
```

Hoặc:
```bash
mvn compile
java -cp target/classes:target/dependency/* com.code.server.AuctionServer
```

**Output mong đợi**:
```
═══════════════════════════════════════
   UET Auction System — Server v1.0
═══════════════════════════════════════
[Server] ✓ DAO khởi tạo xong
[Server] ✓ Service khởi tạo xong
[Server] Lắng nghe trên port 8888...
[Seeder] Tạo dữ liệu mặc định...
[Seeder] ✓ Admin: admin / admin123
[Seeder] ✓ Regular User 1: user1 / user123 (10 triệu VNĐ)
[Seeder] ✓ Regular User 2: user2 / user123 (10 triệu VNĐ)
[Seeder] ✓ Đã tạo 3 sản phẩm mẫu
[Seeder] ✓ Phiên đấu giá RUNNING: iPhone 15 Pro Max
[Seeder] ✓ Seed hoàn tất!
```

#### **5.2 Chạy Client** (terminal/cmd 2)

**Cách 1: Chạy từ IDE**
- Mở file `src/main/java/com/code/ClientApp.java`
- Click `Run` → `Run 'ClientApp'`

**Cách 2: Chạy từ command line**

**Windows**:
```bash
mvn clean javafx:run
```

**macOS / Linux**:
```bash
mvn clean javafx:run
```

Hoặc:
```bash
mvn compile
mvn javafx:run
```

**Output mong đợi**:
- Cửa sổ giao diện JavaFX xuất hiện
- Màn hình Login

### Bước 6: Đăng nhập

**Tài khoản Admin mặc định**:
- Username: `admin`
- Password: `admin123`

**Tạo tài khoản người dùng thông thường**:
- Click "Register"
- Điền thông tin
- Click "Sign Up"

---

## 🌱 Test Data Seeder - Dữ liệu mẫu cho giảng viên

Khi server chạy lần **đầu tiên**, hệ thống tự động khởi tạo dữ liệu mẫu thông qua **DataSeeder** để bạn có thể test ngay mà không cần tạo dữ liệu thủ công.

### 📋 Dữ liệu được khởi tạo:

#### 👤 Tài khoản người dùng:

| Username | Password | Role | Số dư | Mục đích |
|----------|----------|------|-------|---------|
| `admin` | `admin123` | **Admin** | 0 VNĐ | Quản trị viên - quản lý hệ thống |
| `user1` | `user123` | Seller, Bidder | 10.000.000 VNĐ | Người bán & người đấu giá |
| `user2` | `user123` | Seller, Bidder | 10.000.000 VNĐ | Người bán & người đấu giá |

#### 📦 Sản phẩm mẫu:

| Sản phẩm | Loại | Giá khởi điểm | Mô tả | Người bán |
|----------|------|--------------|--------|-----------|
| **iPhone 15 Pro Max** | Electronics | 20.000.000 VNĐ | Điện thoại Apple, 256GB, Bảo hành 12 tháng | user1 |
| **Tranh Sơn Dầu Hoa Sen** | Art | 5.000.000 VNĐ | Tác phẩm gốc, kích thước 60x80cm, Sơn dầu | user1 |
| **Toyota Camry 2022** | Vehicle | 850.000.000 VNĐ | Xe ít đi, còn mới 98%, 51G-12345, đầy đủ giấy tờ | user1 |

#### 🎯 Phiên đấu giá mẫu (RUNNING - Đang diễn ra):

| Chi tiết | Giá trị |
|---------|--------|
| **Sản phẩm** | iPhone 15 Pro Max |
| **Trạng thái** | RUNNING (đang diễn ra) |
| **Giá khởi điểm** | 20.000.000 VNĐ |
| **Bước giá tối thiểu** | 500.000 VNĐ |
| **Thời gian bắt đầu** | 5 phút trước |
| **Thời gian kết thúc** | 2 giờ từ bây giờ |
| **Người tạo phiên** | user1 |



### 🔄 Cách reset dữ liệu Seeder

Nếu muốn chạy lại DataSeeder từ đầu:

```bash
# 1. Drop database
mysql -u root -p
DROP DATABASE auction_db;
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. Chạy server lại
mvn clean compile exec:java@run-server
```

Server sẽ tự động tạo bảng và khởi tạo dữ liệu Seeder.

---

## ✅ Danh sách chức năng đã hoàn thành

### Chức năng Authentication (Xác thực)
- ✅ Đăng ký tài khoản mới
- ✅ Đăng nhập (User/Admin)
- ✅ Đăng xuất
- ✅ Mã hóa password bằng BCrypt
- ✅ Phân quyền (Admin, Seller, Bidder, Moderator)
- ✅ Phiên làm việc (session)

### Chức năng User Management (Quản lý người dùng) - Admin
- ✅ Xem danh sách người dùng
- ✅ Cấm người dùng (ban user)
- ✅ Bỏ cấm người dùng (unban user)
- ✅ Cập nhật thông tin người dùng
- ✅ Cấp role cho người dùng
- ✅ Thu hồi role từ người dùng
- ✅ Xem audit log (lịch sử hoạt động)

### Chức năng Item Management (Quản lý mặt hàng)
- ✅ Tạo mặt hàng đấu giá
- ✅ Chỉnh sửa thông tin mặt hàng
- ✅ Xóa mặt hàng
- ✅ Xem chi tiết mặt hàng
- ✅ Tìm kiếm mặt hàng

### Chức năng Auction (Đấu giá)
- ✅ Tạo phiên đấu giá
- ✅ Khởi động tự động lúc giờ quy định
- ✅ Kết thúc tự động lúc giờ quy định
- ✅ Xem danh sách đấu giá đang diễn ra
- ✅ Xem đấu giá đã kết thúc
- ✅ Theo dõi đấu giá (watching)

### Chức năng Bidding (Đặt giá)
- ✅ Đặt giá cho mặt hàng
- ✅ Kiểm tra giá tối thiểu
- ✅ Kiểm tra số dư ví (balance)
- ✅ Cập nhật giá cao nhất
- ✅ Xem lịch sử đặt giá

### Chức năng Transaction (Giao dịch)
- ✅ Tạo giao dịch khi đấu giá kết thúc
- ✅ Thanh toán (deduct balance)
- ✅ Hoàn lại tiền (refund)
- ✅ Xem lịch sử giao dịch cá nhân
- ✅ Xem tất cả giao dịch (Admin)

### Chức năng Wallet (Ví)
- ✅ Nạp tiền (add balance)
- ✅ Xem số dư
- ✅ Lịch sử giao dịch

### Chức năng Notification (Thông báo)
- ✅ Thông báo khi đấu giá sắp kết thúc
- ✅ Thông báo khi đặt giá thành công
- ✅ Thông báo khi bị đặt giá vượt

### Chức năng System
- ✅ Hệ thống logging (audit log)
- ✅ Xử lý lỗi toàn cục
- ✅ Kiểm tra quyền truy cập

---

## 📊 Sơ đồ Kiến trúc

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT (JavaFX)                        │
│                    SocketClientListener                     │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  GUI Controllers: Login, Dashboard, Auction, Bid, etc.  │ │
│  └────────────────────────────────────────────────────────┘ │
│                          ↑↓                                  │
│                  SocketClient (Socket)                       │
└──────────────────────────┬──────────────────────────────────┘
                           │
                   (TCP Socket:8888)
                           │
┌──────────────────────────↓──────────────────────────────────┐
│                    SERVER (Console)                         │
│                    AuctionServer                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  ClientHandler (ThreadPool) × N clients                │ │
│  │       ↓ RequestProcessor ↓                             │ │
│  │     ↙  ↓  ↓  ↓  ↓  ↓  ↓  ↖                            │ │
│  │  UserService, BidService, AuctionService, etc.         │ │
│  │       ↓  ↓  ↓  ↓  ↓  ↓  ↓                             │ │
│  │  UserDAO, ItemDAO, AuctionDAO, BidDAO, etc.            │ │
│  │       ↓  ↓  ↓  ↓  ↓  ↓  ↓                             │ │
│  │         DBConnection (JDBC)                            │ │
│  └────────────────────────────────────────────────────────┘ │
│                          ↓                                   │
└──────────────────────────┬───────────────────────────────────┘
                           │
                    (MySQL JDBC)
                           │
                       MySQL Server
```

---

## 🛠️ Lệnh Build & Test

### Biên dịch mã nguồn
```bash
mvn clean compile
```

### Chạy unit test
```bash
mvn test
```

### Tạo JAR file
```bash
mvn clean package
```

### Xóa thư mục build
```bash
mvn clean
```

### Chạy linter (Checkstyle)
```bash
mvn checkstyle:check
```

### Tạo báo cáo JaCoCo (code coverage)
```bash
mvn test jacoco:report
# Xem file: target/site/jacoco/index.html
```

---

## 🔐 Bảo mật

- **Password Hashing**: Sử dụng BCrypt (cost factor: 12)
- **Request-Response**: Giao thức đơn giản, có thể nâng cấp thêm SSL/TLS
- **Session Management**: Lưu currentUser trên ClientHandler
- **Access Control**: Kiểm tra role trước mỗi hành động

---

## 📝 Hướng dẫn Dev

### Thêm chức năng mới

1. **Tạo Request type** mới trong `network/RequestType.java`
2. **Thêm xử lý** trong `server/RequestProcessor.java`
3. **Viết Service logic** trong `service/`
4. **Thêm DAO query** nếu cần trong `dao/`
5. **Thêm UI controller** trong `controllers/`
6. **Viết unit test** trong `src/test/`

### Debug

- **Server**: Xem console output, log lệnh SQL
- **Client**: Xem dialog error, check config.properties
- **Database**: Dùng MySQL Workbench để query

---

## ⚠️ Troubleshooting

### 1. "Không thể kết nối MySQL"
- ✅ Kiểm tra MySQL đang chạy: `mysql -u root -p`
- ✅ Kiểm tra user/password trong `db.properties`
- ✅ Kiểm tra database `auction_db` tồn tại

### 2. "Không thể kết nối Server"
- ✅ Kiểm tra Server đang chạy (tìm port 8888 lắng nghe)
- ✅ Kiểm tra IP/port trong `config.properties` đúng
- ✅ Firewall cho phép port 8888

### 3. "Login.fxml không tìm thấy"
- ✅ Chạy `mvn clean package` để copy resources
- ✅ Kiểm tra file tồn tại: `src/main/resources/com/code/views/Login.fxml`

### 4. "Java version mismatch"
- ✅ Kiểm tra: `java -version`
- ✅ Phải là Java 21+
- ✅ Cập nhật JAVA_HOME nếu cần

---

## 📚 Tài liệu bổ sung

- [Báo cáo PDF](link-to-pdf) *(Cần cập nhật)*
- [Video Demo](link-to-video) *(Cần cập nhật)*

---
