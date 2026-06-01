<div align="center">
  <h1>🎬 WebPhim - Cinema Booking System (Java Spring Boot + React)</h1>
  <p>Hệ thống đặt vé xem phim trực tuyến hiện đại với đầy đủ tính năng dành cho Khách hàng & Ban Quản Trị.</p>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=java" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3-6DB33F?logo=spring-boot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/React-19-61DAFB?logo=react" alt="React" />
  <img src="https://img.shields.io/badge/Database-MySQL-4479A1?logo=mysql" alt="MySQL" />
  <img src="https://img.shields.io/badge/Cache-Redis-DC382D?logo=redis" alt="Redis" />
</div>

<hr/>

## 📖 Tổng quan
**WebPhim** là nền tảng quản lý rạp chiếu phim và đặt vé trực tuyến. Frontend được xây dựng bằng **React + Vite**, Backend được xây dựng bằng **Java Spring Boot**.

## 🛠️ Hướng dẫn cài đặt & Chạy dự án (Local Development)

### 1. Yêu cầu Hệ thống
- **Java 21**
- **Node.js** và **npm**
- **Docker Desktop** (để chạy MySQL & Redis)

*(Lưu ý: Không cần cài Maven riêng do dự án đã có sẵn Maven Wrapper `./mvnw`)*

### 2. Thiết lập Database & Caching (Docker)
Mở terminal tại thư mục backend:
```bash
cd Backe/DotnetBackend
```
*(Lưu ý: Thư mục mang tên `DotnetBackend` nhưng hiện tại chứa mã nguồn Java Spring Boot)*

Khởi tạo cấu hình biến môi trường `.env` cho Docker Compose:
```bash
cat > .env <<'EOF'
DB_NAME=webphim
DB_USER=root
DB_PASSWORD=webphim123
DB_PORT=3307
REDIS_PASSWORD=dxh1234
EOF
```

Chạy các service MySQL và Redis (yêu cầu Docker Desktop đang hoạt động):
```bash
docker compose up -d
```

### 3. Chạy Backend (Java Spring Boot)
Vẫn tại thư mục `Backe/DotnetBackend`, khai báo các biến môi trường cho Spring Boot:
```bash
export DB_HOST=127.0.0.1
export DB_PORT=3307
export DB_NAME=webphim
export DB_USER=root
export DB_PASSWORD=webphim123
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6380
export REDIS_PASSWORD=dxh1234
export JWT_SECRET=change-this-development-secret-change-this-development-secret
```

Khởi động Backend:
```bash
./mvnw spring-boot:run
```
*Backend sẽ khởi chạy và lắng nghe tại: `http://localhost:8080`*

### 4. Nạp Dữ Liệu Mẫu
Sau khi Backend chạy thành công lần đầu tiên để tự động tạo schema Database, tiến hành nạp dữ liệu mẫu. Mở terminal mới:
```bash
cd Backe/DotnetBackend
docker exec -i webphim-mysql mysql --default-character-set=utf8mb4 -uroot -pwebphim123 webphim < final-fixed-test-data.sql
```

### 5. Chạy Frontend (React/Vite)
Mở một terminal khác tại thư mục frontend:
```bash
cd Fronte/vite-project
npm install
npm run dev
```
*Frontend sẽ khởi chạy tại: `http://localhost:5173` và tự động thiết lập proxy gọi API sang `http://localhost:8080`.*
