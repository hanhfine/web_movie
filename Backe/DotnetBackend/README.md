# WebPhim Java Backend

Backend đã được chuyển từ ASP.NET Core sang Java Spring Boot, giữ nguyên các route `/api/...` để frontend hiện tại tiếp tục sử dụng.

## Chạy backend

```bash
cd Backe/DotnetBackend
docker compose up -d
./mvnw spring-boot:run
```

Các biến môi trường chính:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `JWT_SECRET`
- `SEPAY_WEBHOOK_SECRET`

## Kiểm tra

```bash
./mvnw test
./mvnw package
```
