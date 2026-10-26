# Cần cứu trợ — Website điều phối cứu trợ khẩn cấp

Ứng dụng web kết nối người cần cứu trợ (Victim), người cứu trợ (Rescuer) và quản trị viên (Admin). Victim gửi yêu cầu kèm vị trí; Rescuer lọc, nhận và cập nhật trạng thái; Admin giám sát tài khoản, yêu cầu và thống kê dạng thẻ số.


Đặc tả đầy đủ: [docs/SPEC.md](docs/SPEC.md). Kiến trúc hiện tại: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Danh sách các trang chính

- **Công khai (Khách vãng lai):**
  - Trang chủ giới thiệu & thống kê cộng đồng: `/`
  - Đăng ký tài khoản cần cứu trợ: `/register/victim`
  - Đăng ký tài khoản tình nguyện viên / đội cứu trợ: `/register/rescuer`
  - Gửi yêu cầu cứu trợ khẩn cấp nhanh (SOS không cần đăng nhập): `/emergency`
  - Đăng nhập hệ thống: `/login`
- **Người cần cứu trợ (Victim):**
  - Quản lý danh sách yêu cầu của tôi: `/victim/home`
  - Tạo yêu cầu cứu trợ mới: `/victim/requests/new`
  - Xem chi tiết và tiến độ xử lý yêu cầu: `/victim/requests/{id}`
  - Cập nhật hồ sơ cá nhân và đổi mật khẩu: `/profile`
- **Người / Đội cứu trợ (Rescuer):**
  - Bảng tin yêu cầu chờ tiếp nhận (lọc trạng thái, nhu cầu, mức độ): `/rescuer/home`
  - Xem chi tiết yêu cầu và nhận hỗ trợ: `/rescuer/requests/{id}`
  - Danh sách nhiệm vụ đang phụ trách: `/rescuer/tasks`
  - Chi tiết nhiệm vụ và cập nhật tiến trình (tiếp nhận -> đang xử lý -> hoàn thành): `/rescuer/tasks/{id}`
  - Cập nhật hồ sơ cá nhân và đổi mật khẩu: `/profile`
- **Quản trị viên (Admin):**
  - Tổng quan điều phối & 8 thẻ thống kê yêu cầu: `/admin/home`
  - Quản lý và phê duyệt tài khoản (Victim / Rescuer, duyệt hàng loạt): `/admin/users`
  - Xem hồ sơ chi tiết và lịch sử yêu cầu của nạn nhân: `/admin/users/{id}`
  - Cập nhật hồ sơ cá nhân và đổi mật khẩu: `/profile`


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

> **Lưu ý về đăng ký:** Victim đăng ký xong đăng nhập ngay được. Rescuer đăng ký xong tài khoản ở trạng thái chờ duyệt (`enabled=false`); Admin phải vào trang quản lý tài khoản bấm "Mở khóa" thì Rescuer mới đăng nhập được.

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
