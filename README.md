# Website điều phối cứu trợ thiên tai

Ứng dụng web kết nối người cần cứu trợ (Victim), người cứu trợ (Rescuer) và quản trị viên (Admin). Victim gửi yêu cầu kèm vị trí; Rescuer lọc, nhận và cập nhật trạng thái; Admin giám sát tài khoản, yêu cầu và thống kê dạng thẻ số.

Đặc tả đầy đủ: [docs/SPEC.md](docs/SPEC.md). Kiến trúc hiện tại: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Công nghệ

- Java 21, Maven, Spring Boot 4.1.1
- Spring Security 7, Spring Data JPA, Thymeleaf
- PostgreSQL (Neon)
- Bootstrap 5 (CDN)

Không dùng React, Vue hay REST SPA.

## Yêu cầu

- JDK 21
- Tài khoản được mời vào project PostgreSQL Neon của nhóm (lấy `DB_URL`, `DB_USER`, `DB_PASSWORD` từ nhóm trưởng; không commit file `.env`)

## Chạy lần đầu

1. Clone repository.
2. Tạo file `.env` từ `.env.example` và điền `DB_URL`, `DB_USER`, `DB_PASSWORD`.
3. Cài và biên dịch (bỏ qua test):

   ```bash
   ./mvnw clean install -DskipTests
   ```

4. Chạy ứng dụng:

   ```bash
   ./mvnw spring-boot:run
   ```

5. Mở [http://localhost:8080/login](http://localhost:8080/login).

Trên Windows có thể dùng `mvnw.cmd` thay cho `./mvnw`.

## Tài khoản mẫu (chỉ dùng demo)

Khi database chưa có user, `config/DataInitializer.java` seed các tài khoản sau. Mật khẩu lưu dạng BCrypt; bảng dưới là giá trị plaintext dùng khi seed.

| Vai trò | Email | Mật khẩu |
|---------|-------|----------|
| Admin | `admin@relief.vn` | `admin123` |
| Rescuer | `rescuer1@relief.vn` | `rescuer123` |
| Rescuer | `rescuer2@relief.vn` | `rescuer123` |
| Victim | `victim1@relief.vn` | `victim123` |
| Victim | `victim2@relief.vn` | `victim123` |
| Victim | `victim3@relief.vn` | `victim123` |

## Cấu trúc thư mục

```
relief-coordination/
├── docs/                  SPEC.md, ARCHITECTURE.md
├── src/main/java/.../     Spring Boot
│   ├── config/            Security, PasswordEncoder, DataInitializer
│   ├── controller/        Auth, Home, Victim, Rescuer, Admin
│   ├── model/             User (abstract), subtype, ReliefRequest, enum
│   ├── repository/
│   └── security/          CustomUserDetailsService
├── src/main/resources/
│   ├── application.properties
│   └── templates/         auth/login, fragments/layout, {admin,rescuer,victim}/home
└── src/test/java/         contextLoads
```
