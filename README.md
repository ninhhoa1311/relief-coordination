# Cần cứu trợ

Nền tảng web điều phối cứu trợ thiên tai, kết nối người cần hỗ trợ (**Victim**) với người đến cứu trợ (**Rescuer**). Hệ thống quản lý toàn bộ vòng đời một yêu cầu cứu trợ — từ lúc gửi đến khi xác nhận hoàn thành — và cho phép **Admin** giám sát tài khoản, yêu cầu và số liệu thống kê.

> Dự án môn Lập trình hướng đối tượng (OOP). Tên kỹ thuật (artifactId Maven): `relief-coordination`.

---

## Mục lục tài liệu

| File | Nội dung |
|------|----------|
| [docs/SPEC.md](docs/SPEC.md) | Đặc tả yêu cầu, nghiệp vụ, quy tắc chuyển trạng thái |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Kiến trúc phân tầng, luồng xử lý request, bảo mật |
| [docs/OOP-DESIGN.md](docs/OOP-DESIGN.md) | Thiết kế OOP: kế thừa, đóng gói, đa hình, sơ đồ lớp |
| [docs/DATABASE.md](docs/DATABASE.md) | Cấu trúc bảng, quan hệ, sơ đồ ER |
| [docs/WORKFLOW.md](docs/WORKFLOW.md) | Sơ đồ trạng thái, sơ đồ tuần tự |
| [docs/ROUTES.md](docs/ROUTES.md) | Toàn bộ route và phân quyền |
| [docs/USER-GUIDE.md](docs/USER-GUIDE.md) | Hướng dẫn sử dụng cho từng vai trò |
| [docs/TESTING.md](docs/TESTING.md) | Test hiện có và kịch bản kiểm thử thủ công |
| [docs/README.md](docs/README.md) | Mục lục tài liệu |

---

## Tính năng chính

### Khách chưa đăng nhập
- Xem trang Landing với số liệu thống kê tổng quan (tổng yêu cầu, hoàn thành, người được hỗ trợ).
- Gửi **yêu cầu SOS khẩn cấp** tại `/emergency` mà không cần đăng nhập: nhập tên và số điện thoại, tùy chọn chia sẻ vị trí GPS. SOS tự động đặt mức khẩn cấp **CRITICAL** và loại nhu cầu **Cứu người**. Chống spam: cùng số điện thoại đã có yêu cầu đang hoạt động sẽ không tạo trùng.
- Đăng ký tài khoản Victim (`/register/victim`) hoặc Rescuer (`/register/rescuer`).
- Đăng nhập (`/login`).

### Victim (người cần cứu trợ)
- Tạo yêu cầu cứu trợ: địa điểm (text), chọn nhiều loại nhu cầu (Thực phẩm / Nước uống / Y tế / Nơi ở / Cứu người / Khác), số người cần hỗ trợ, mức độ khẩn cấp (Thấp / Trung bình / Cao / Khẩn cấp), mô tả.
- Tùy chọn lấy vị trí GPS bằng Geolocation API trình duyệt (một lần khi bấm nút); GPS không bắt buộc.
- Xem danh sách và chi tiết tất cả yêu cầu của mình.
- Sửa và hủy yêu cầu **chỉ khi còn trạng thái SENT**.
- Theo dõi trạng thái; sau khi Rescuer nhận, xem được tên và SĐT của Rescuer phụ trách.
- Khi yêu cầu COMPLETED, xác nhận đã nhận hỗ trợ → chuyển sang CONFIRMED.
- Cập nhật hồ sơ cá nhân (họ tên, SĐT, đổi mật khẩu) tại `/profile`.

### Rescuer (người cứu trợ)
- Sau khi đăng ký cần chờ Admin duyệt tài khoản mới đăng nhập được.
- Xem danh sách yêu cầu đang chờ (SENT), lọc theo địa điểm / loại nhu cầu / mức khẩn cấp, sắp xếp theo thời gian tạo; phân trang 10 kết quả/trang.
- Xem chi tiết yêu cầu trước khi nhận (ẩn tên và SĐT Victim cho đến khi nhận).
- Nhận nhiệm vụ (một Rescuer được nhận nhiều yêu cầu cùng lúc); chống nhận trùng bằng optimistic locking (`@Version`).
- Trang "Nhiệm vụ của tôi": xem, lọc, sắp xếp nhiệm vụ đã nhận; cập nhật trạng thái `ACCEPTED → PROCESSING → COMPLETED`.
- Xem thống kê cá nhân (đang làm / hoàn thành / tổng).
- Cập nhật hồ sơ cá nhân tại `/profile`.

### Admin (quản trị viên)
- Dashboard tích hợp thống kê **8 thẻ số**: Tổng yêu cầu, Đã gửi, Đã tiếp nhận, Đang xử lý, Hoàn thành, Đã xác nhận, Đã hủy, Số người được hỗ trợ.
- Xem và lọc toàn bộ yêu cầu (theo trạng thái, mức khẩn cấp, loại nhu cầu, từ khóa); phân trang 10 kết quả/trang; yêu cầu khẩn cấp chưa xử lý nổi lên đầu.
- Hủy bất kỳ yêu cầu nào ngoại trừ đã CONFIRMED hoặc đã CANCELLED.
- Xem danh sách Victim và Rescuer; khóa / mở khóa tài khoản (không khóa được Admin).
- Duyệt Rescuer mới đăng ký (từng người hoặc duyệt tất cả).
- Xem chi tiết một Victim: thông tin cá nhân và lịch sử yêu cầu có phân trang.

---

## Công nghệ sử dụng

| Thành phần | Công nghệ / Phiên bản |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Bảo mật | Spring Security 7 (tích hợp sẵn trong Boot 4) |
| ORM | Spring Data JPA / Hibernate |
| Template | Thymeleaf + thymeleaf-extras-springsecurity6 |
| Validation | spring-boot-starter-validation |
| Database | PostgreSQL (Neon serverless) |
| Build | Maven (Maven Wrapper `mvnw`) |
| Utility | Lombok |
| Dev | Spring DevTools |
| Giao diện | Bootstrap 5.3.3 (CDN) + Bootstrap Icons 1.11.3 (CDN) + Google Fonts Be Vietnam Pro |
| CSS | `app.css` — design system dùng chung |

---

## Yêu cầu cài đặt

- **JDK 21** trở lên (khuyến nghị Eclipse Temurin 21 LTS).
- **Maven Wrapper** đã có sẵn (`mvnw` / `mvnw.cmd`) — không cần cài Maven riêng.
- Tài khoản **PostgreSQL trên Neon** (hoặc PostgreSQL local): mỗi thành viên dùng một Neon branch riêng.
- Git.

---

## Hướng dẫn chạy

### Bước 1 — Clone repository

```bash
git clone <url-repository>
cd relief-coordination
```

### Bước 2 — Tạo file `.env` từ mẫu

Sao chép file mẫu:

```bash
# macOS/Linux
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

Mở file `.env` và điền thông tin kết nối của Neon branch của bạn:

```
DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require
DB_USER=<tên-người-dùng>
DB_PASSWORD=<mật-khẩu>
```

Giải thích từng biến:

| Biến | Ý nghĩa |
|---|---|
| `DB_URL` | JDBC URL kết nối PostgreSQL. Lấy từ Neon Console → Connection Details → chọn branch của bạn → copy JDBC URL. |
| `DB_USER` | Tên người dùng PostgreSQL (thường là `neondb_owner`). |
| `DB_PASSWORD` | Mật khẩu tương ứng. |

> **Lưu ý:** File `.env` đã có trong `.gitignore` — **không bao giờ commit file `.env`**.

### Bước 3 — Lần đầu chạy: kiểm tra migration SQL

Nếu Neon branch của bạn còn dữ liệu cũ (từ trước khi `needTypes` đổi sang bảng nhiều-nhiều), hãy chạy file migration trước khi khởi động ứng dụng:

```sql
-- Chạy trên Neon SQL Editor của branch của bạn
-- Xem chi tiết trong docs/migration-need-types.sql
```

Xem hướng dẫn chi tiết tại [Lưu ý Database](#lưu-ý-về-database-neon).

### Bước 4 — Chạy ứng dụng

**macOS / Linux:**
```bash
./mvnw spring-boot:run
```

**Windows PowerShell:**
```powershell
.\mvnw.cmd spring-boot:run
```

Ứng dụng khởi động tại: **http://localhost:8080**

### Bước 5 — Chạy test

```bash
# macOS/Linux
./mvnw test

# Windows PowerShell
.\mvnw.cmd test
```

> Test dùng `@SpringBootTest` + `@Transactional` (tự rollback sau mỗi test), cần kết nối đến database thật trong `.env`.

---

## Tài khoản demo

Các tài khoản sau được tạo tự động bởi `DataInitializer` khi bảng `users` trống (lần đầu chạy). Chỉ dùng cho mục đích demo, không dùng trong môi trường production.

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Admin | `admin@relief.vn` | `admin123` |
| Rescuer | `rescuer1@relief.vn` | `rescuer123` |
| Rescuer | `rescuer2@relief.vn` | `rescuer123` |
| Victim | `victim1@relief.vn` | `victim123` |
| Victim | `victim2@relief.vn` | `victim123` |
| Victim | `victim3@relief.vn` | `victim123` |

Ngoài ra, `DataInitializer` tạo sẵn **20 yêu cầu mẫu** (đủ mọi trạng thái) và **2 yêu cầu SOS mẫu** (victim = null, để demo tính năng SOS). Muốn seed lại: chạy `TRUNCATE TABLE users RESTART IDENTITY CASCADE;` trên Neon branch của bạn rồi khởi động lại app.

---

## Lưu ý về Database (Neon)

Dự án dùng **1 Neon project chung**, mỗi thành viên tạo một **Neon branch riêng** để phát triển độc lập.

### File migration hiện có

| File | Mô tả | Khi nào cần chạy |
|---|---|---|
| [`docs/migration-need-types.sql`](docs/migration-need-types.sql) | Chuyển `needType` (cột đơn) sang bảng `relief_request_need_types` (nhiều-nhiều) + xóa cột cũ | Cần chạy trên **mỗi Neon branch** nếu branch đó có dữ liệu cũ trước khi cột `need_type` bị xóa |

Xem nội dung và hướng dẫn chi tiết trong file SQL.

---

## Cấu trúc thư mục

```
relief-coordination/
├── src/
│   ├── main/
│   │   ├── java/com/reliefcoordination/relief_coordination/
│   │   │   ├── config/          # SecurityConfig, PasswordEncoderConfig, DataInitializer
│   │   │   ├── controller/      # Controller theo từng role (Victim, Rescuer, Admin, Auth, Home, Emergency, Profile, Error)
│   │   │   │   └── form/        # Form object cho đăng ký và hồ sơ (RegisterForm, ProfileForm)
│   │   │   ├── dto/             # DTO: ReliefRequestForm, EmergencyRequestDto, PublicStatsDto
│   │   │   ├── exception/       # GlobalExceptionHandler, ResourceNotFoundException
│   │   │   ├── model/           # Entity JPA: User (abstract), Victim, Rescuer, Admin, ReliefRequest
│   │   │   │                    # Enum: RequestStatus, NeedType, UrgencyLevel
│   │   │   ├── repository/      # Spring Data JPA Repository (interface)
│   │   │   ├── security/        # CustomUserDetailsService
│   │   │   ├── service/         # Service theo chức năng: VictimRequestService, RescuerRequestService,
│   │   │   │                    # RescuerTaskService, AdminService, EmergencyService, PublicStatsService,
│   │   │   │                    # CurrentUserService
│   │   │   └── util/            # (trống hoặc tiện ích nếu có)
│   │   └── resources/
│   │       ├── templates/       # Thymeleaf HTML
│   │       │   ├── fragments/   # layout.html (khung dùng chung), nav-*.html, status-badge, urgency-badge,
│   │       │   │                # need-badges, urgency-picker, progress-steps, back-home
│   │       │   ├── auth/        # login.html, register-victim.html, register-rescuer.html
│   │       │   ├── victim/      # home.html, request-form.html, request-detail.html
│   │       │   ├── rescuer/     # home.html, request-detail.html, tasks.html, task-detail.html
│   │       │   ├── admin/       # home.html, users.html, victim-detail.html
│   │       │   ├── emergency/   # form.html, success.html
│   │       │   ├── profile/     # edit.html
│   │       │   ├── error/       # 403.html, 404.html, 500.html, error.html
│   │       │   └── landing.html # Trang chủ công khai
│   │       ├── static/css/      # app.css — design system dùng chung
│   │       └── application.properties
│   └── test/
│       └── java/...service/     # Test: AdminServiceTest, VictimRequestServiceTest,
│                                # RescuerRequestServiceTest, RescuerTaskServiceTest, PublicStatsServiceTest
├── docs/                        # Tài liệu dự án
│   ├── migration-need-types.sql # Migration SQL
│   ├── SPEC.md                  # Đặc tả yêu cầu
│   ├── ARCHITECTURE.md          # Kiến trúc hệ thống
│   ├── OOP-DESIGN.md            # Thiết kế OOP
│   ├── DATABASE.md              # Cơ sở dữ liệu
│   ├── WORKFLOW.md              # Luồng nghiệp vụ
│   ├── ROUTES.md                # Danh sách route
│   ├── USER-GUIDE.md            # Hướng dẫn sử dụng
│   ├── TESTING.md               # Kiểm thử
│   └── README.md                # Mục lục tài liệu
├── .env.example                 # Mẫu biến môi trường (commit được)
├── .env                         # Biến môi trường thật (git-ignored, KHÔNG commit)
├── AGENTS.md                    # Quy tắc cho AI agent
├── pom.xml                      # Maven build
└── mvnw / mvnw.cmd              # Maven Wrapper
```

---

## Quy ước làm việc nhóm

- **Nhánh:** mỗi người tạo nhánh `feature/<tên>-<chức-năng>` (ví dụ: `feature/an-victim-crud`), merge vào `main` qua Pull Request.
- **Database:** mỗi người dùng một Neon branch riêng, file `.env` riêng, **không commit `.env`**.
- **Quy tắc code quan trọng:** xem [AGENTS.md](AGENTS.md) và [docs/SPEC.md](docs/SPEC.md) mục 0 và 8.
- File được bảo vệ (chỉ nhóm trưởng sửa): `pom.xml`, `SecurityConfig.java`, `application.properties`, toàn bộ `model/`, `layout.html`, `DataInitializer.java`.

---

## Thành viên nhóm

| Họ tên | MSSV | Vai trò / Phụ trách |
|---|---|---|
| | | |
| | | |
| | | |
| | | |
| | | |

<!-- TODO: nhóm tự điền bảng này -->

---

## Liên kết nhanh

- [Đặc tả yêu cầu](docs/SPEC.md) · [Kiến trúc](docs/ARCHITECTURE.md) · [OOP Design](docs/OOP-DESIGN.md) · [Database](docs/DATABASE.md) · [Workflow](docs/WORKFLOW.md) · [Routes](docs/ROUTES.md) · [User Guide](docs/USER-GUIDE.md) · [Testing](docs/TESTING.md)
