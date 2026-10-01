# Relief Coordination — kiến trúc hiện tại

Spring Boot điều phối cứu trợ: Victim gửi yêu cầu, Rescuer nhận/xử lý, Admin quản trị. Đã có domain, JPA, security, seed data, 5 controller và 4 template Thymeleaf. Chưa có tầng service hay CRUD yêu cầu.

## 1. Phiên bản và dependency

- Java 21, Spring Boot 4.1.1, Maven, artifact `relief-coordination` `0.0.1-SNAPSHOT`.
- Chính: `spring-boot-starter-webmvc`, `data-jpa`, `security`, `thymeleaf`, `validation`.
- Khác: `thymeleaf-extras-springsecurity6`, PostgreSQL (runtime), Lombok, DevTools (optional).
- Test: các starter `*-test` tương ứng (JPA, Security, Thymeleaf, Validation, WebMVC).
- Cấu hình: port 8080; datasource từ biến môi trường; JPA `ddl-auto=update`, `open-in-view=false`; Thymeleaf cache tắt.

## 2. Cây package và vai trò class

Gốc: `com.reliefcoordination.relief_coordination`

- `ReliefCoordinationApplication` — bootstrap.
- **config**
  - `SecurityConfig` — filter chain, form login, phân quyền theo prefix URL.
  - `PasswordEncoderConfig` — bean BCrypt.
  - `DataInitializer` — seed 1 admin, 2 rescuer, 3 victim và 20 `ReliefRequest` nếu bảng `users` trống.
- **controller** (5 class)
  - `AuthController` — GET `/login` → `auth/login` (không dùng layout).
  - `HomeController` — GET `/home` redirect theo `ROLE_ADMIN` / `ROLE_RESCUER` / `ROLE_VICTIM`.
  - `VictimController` — `@RequestMapping("/victim")`, GET `/home`.
  - `RescuerController` — `@RequestMapping("/rescuer")`, GET `/home`.
  - `AdminController` — `@RequestMapping("/admin")`, GET `/home`.
- **model** — `User` (abstract, JOINED); `Admin` / `Victim` / `Rescuer`; `ReliefRequest`; enum `RequestStatus`, `NeedType`, `UrgencyLevel`.
- **repository** — `UserRepository` (`findByEmail`); subtype CRUD không query email; `ReliefRequestRepository` (`findByStatus|Victim|Rescuer`, `countByStatus`).
- **security** — `CustomUserDetailsService` map `ROLE_ADMIN|RESCUER|VICTIM`.
- **test** — `ReliefCoordinationApplicationTests` (`contextLoads`).

Chưa có package `service` / `dto`.

## 3. Template Thymeleaf (4 file)

Thư mục `src/main/resources/templates/`:

| File | Vai trò |
|------|---------|
| `auth/login.html` | Form login độc lập, Bootstrap 5 CDN |
| `fragments/layout.html` | Navbar, logout CSRF, `th:replace="~{${content} :: content}"` |
| `victim/home.html` | Fragment `content` trang chủ Victim |
| `rescuer/home.html` | Fragment `content` trang chủ Rescuer |
| `admin/home.html` | Fragment `content` trang chủ Admin |

Ba trang home nhận `content` và `pageTitle` từ controller tương ứng, trả view `fragments/layout`.

## 4. Entity, quan hệ, kế thừa

**User (abstract)** — JOINED, bảng `users`: `id`, `fullName`, `email` (unique), `password`, `phoneNumber`, `enabled` (mặc định true).  
**Admin / Victim / Rescuer** — chỉ kế thừa User (bảng `admins` / `victims` / `rescuers`).

**ReliefRequest** — `id`; `@ManyToOne` bắt buộc `Victim`; `@ManyToOne` tùy chọn `Rescuer`; `location`; `latitude`/`longitude` (optional); `needType`; `numberOfPeople`; `urgencyLevel`; `description` (max 1000); `status` mặc định `SENT`; `createdAt`; `updatedAt`; `@Version`. Không `@OneToMany` ngược; không cascade/orphanRemoval.

## 5. Enum và chuyển trạng thái

- `NeedType`: FOOD, WATER, MEDICAL, SHELTER, RESCUE, OTHER.
- `UrgencyLevel`: LOW, MEDIUM, HIGH, CRITICAL.
- `RequestStatus`: SENT → ACCEPTED → PROCESSING → COMPLETED → CONFIRMED; CANCELLED từ SENT/ACCEPTED/PROCESSING.
  - SENT → ACCEPTED \| CANCELLED
  - ACCEPTED → PROCESSING \| CANCELLED
  - PROCESSING → COMPLETED \| CANCELLED
  - COMPLETED → CONFIRMED
  - CONFIRMED, CANCELLED: kết thúc.
- `canTransitionTo(next)` có trên enum; **chưa** gọi từ service (chưa có service).

## 6. Endpoint hiện có

| URL | Method | Handler | Role |
|-----|--------|---------|------|
| `/login` | GET | `AuthController` | public |
| `/login` | POST | form login Spring | public |
| `/logout` | POST | Spring mặc định | đã đăng nhập |
| `/home` | GET | `HomeController` → redirect | authenticated |
| `/victim/home` | GET | `VictimController` | ROLE_VICTIM |
| `/rescuer/home` | GET | `RescuerController` | ROLE_RESCUER |
| `/admin/home` | GET | `AdminController` | ROLE_ADMIN |

Public còn khai báo: `/`, `/register`, `/css/**`, `/js/**` (chưa có view register).

## 7. Cấu hình Security

- Public: `/`, `/register`, `/login`, static `/css/**` `/js/**`; form login + logout `permitAll`.
- `hasRole` theo prefix `/victim/**`, `/rescuer/**`, `/admin/**`.
- Login page `/login`, thành công luôn `/home`; logout → `/login?logout`.
- CSRF bật; session form-login (không JWT).
- Auth: `DaoAuthenticationProvider` + BCrypt + `CustomUserDetailsService` (role theo `instanceof` subtype; `disabled` nếu `!enabled`).
- Lookup email: `findAll()` rồi filter Admin → Rescuer → Victim; **không** dùng `UserRepository.findByEmail`.

## 8. Đã xong / đang dở / rủi ro

**Xong:** model JOINED; enum + transition map; repository; BCrypt; SecurityFilterChain; seed; login/logout; redirect `/home` theo role; layout + 3 trang home placeholder.

**Dở:** không đăng ký; không CRUD yêu cầu; không service nên chưa enforce `canTransitionTo` / ownership / `@Version`; chưa DTO/validation form; subtype repo thiếu `findByEmail`. Sau này tách `RescuerController` thành `RescuerRequestController` và `RescuerTaskController` (xem SPEC).

**Rủi ro:** `CustomUserDetailsService` load toàn bảng; `UserRepository.findByEmail` chưa dùng; `ddl-auto=update`; test mới `contextLoads`.
