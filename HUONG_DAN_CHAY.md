# Hướng Dẫn Chạy Dự Án WebPhim Java

File này dùng cho repo Java mới tại:

```bash
/Users/nguyenhoangduong/Documents/webphim-java
```

Frontend vẫn giữ nguyên React/Vite. Backend đã chuyển sang Java Spring Boot và vẫn dùng các route `/api/...` như bản cũ.

## 1. Yêu Cầu

Cần có:

- Java 21
- Docker Desktop
- Node.js và npm

Kiểm tra nhanh:

```bash
java -version
docker --version
node -v
npm -v
```

Không cần cài Maven riêng vì backend đã có Maven Wrapper `./mvnw`.

## 2. Chạy MySQL Và Redis

Mở Docker Desktop trước và chờ đến khi Docker báo đang chạy.

Mở terminal tại backend:

```bash
cd /Users/nguyenhoangduong/Documents/webphim-java/Backe/DotnetBackend
```

Tạo file `.env` cho Docker Compose:

```bash
cat > .env <<'EOF'
DB_NAME=webphim
DB_USER=root
DB_PASSWORD=webphim123
DB_PORT=3307
REDIS_PASSWORD=dxh1234
EOF
```

Khởi động MySQL và Redis:

```bash
docker compose up -d
```

Kiểm tra container:

```bash
docker ps
```

## 3. Chạy Backend Java

Vẫn ở thư mục:

```bash
/Users/nguyenhoangduong/Documents/webphim-java/Backe/DotnetBackend
```

Export biến môi trường cho Spring Boot:

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

Chạy backend:

```bash
./mvnw spring-boot:run
```

Backend chạy tại:

```text
http://localhost:8080
```

Test nhanh:

```bash
curl http://localhost:8080/api/movies
```

## 4. Nạp Dữ Liệu Mẫu

Nên chạy backend một lần trước để Spring Boot tạo/cập nhật bảng. Sau đó mở terminal mới tại backend và chạy:

```bash
cd /Users/nguyenhoangduong/Documents/webphim-java/Backe/DotnetBackend
docker exec -i webphim-mysql mysql --default-character-set=utf8mb4 -uroot -pwebphim123 webphim < final-fixed-test-data.sql
```

Nếu muốn reset database hoàn toàn bằng file dump đầy đủ:

```bash
docker exec -i webphim-mysql mysql --default-character-set=utf8mb4 -uroot -pwebphim123 webphim < Dump20260419.sql
```

Sau khi import xong, gọi lại:

```bash
curl http://localhost:8080/api/movies
```

## 5. Chạy Frontend

Mở terminal khác:

```bash
cd /Users/nguyenhoangduong/Documents/webphim-java/Fronte/vite-project
npm install
npm run dev
```

Frontend chạy tại:

```text
http://localhost:5173
```

Frontend đang dùng `VITE_API_BASE_URL=/api`, và Vite proxy sẽ forward `/api` sang backend:

```text
http://127.0.0.1:8080
```

## 6. Lệnh Kiểm Tra Backend

Tại thư mục backend:

```bash
./mvnw test
./mvnw package
```

## 7. Lệnh Build Frontend

Tại thư mục frontend:

```bash
npm run build
```

## 8. Dừng Dịch Vụ

Dừng backend bằng `Ctrl + C`.

Dừng MySQL và Redis:

```bash
cd /Users/nguyenhoangduong/Documents/webphim-java/Backe/DotnetBackend
docker compose down
```

Nếu muốn xóa luôn dữ liệu Docker volume:

```bash
docker compose down -v
```

## 9. Lỗi Thường Gặp

Nếu backend báo không kết nối được MySQL, kiểm tra lại `DB_PORT=3307`. Docker Compose expose MySQL ra máy host ở cổng `3307`.

Nếu `docker compose up -d` báo `Cannot connect to the Docker daemon`, nghĩa là Docker Desktop chưa chạy. Mở ứng dụng Docker Desktop, chờ khởi động xong rồi chạy lại lệnh.

Nếu Redis báo sai mật khẩu, kiểm tra `REDIS_PASSWORD=dxh1234`.

Nếu frontend gọi API lỗi, kiểm tra backend đã chạy ở `http://localhost:8080` và frontend đang chạy bằng `npm run dev`, không mở trực tiếp file HTML.

Nếu cookie đăng nhập không lưu khi chạy HTTP local, có thể do cookie đang cấu hình `Secure` và `SameSite=None` để tương thích deploy HTTPS. Khi test local, ưu tiên dùng token trong response hoặc chỉnh cấu hình cookie nếu cần debug đăng nhập sâu.
