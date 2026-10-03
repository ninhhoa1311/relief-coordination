# Relief Coordination — kiến trúc hiện tại

Spring Boot điều phối cứu trợ: Victim gửi yêu cầu, Rescuer nhận/xử lý, Admin quản trị. Đã có domain, JPA, security, seed data, 6 controller và 5 template Thymeleaf. Chưa có tầng service hay CRUD yêu cầu.

## 1. Phiên bản và dependency

- Java 21, Spring Boot 4.1.1, Maven, artifact `relief-coordination` `0.0.1-SNAPSHOT`.
- Chính: `spring-boot-starter-webmvc`, `data-jpa`, `security`, `thymeleaf`, `validation`.
- Khác: `thymeleaf-extras-springsecurity6`, PostgreSQL (runtime), Lombok, DevTools (optional).
- Test: các starter `*-test` tương ứng (JPA, Security, Thymeleaf, Validation, WebMVC).
- Cấu hình: port 8080; `spring.config.import` file `.env` (optional); datasource từ biến môi trường; JPA `ddl-auto=update`, `open-in-view=false`, `show-sql` bật; còn `spring.jpa.database-platform`; không có key `hibernate.dialect`; Thymeleaf cache tắt.

## 2. Cây package và vai trò class

Gốc: `com.reliefcoordination.relief_coordination`

- `ReliefCoordinationApplication` — bootstrap.
- **config**
  - `SecurityConfig` — filter chain, form login, phân quyền theo prefix URL.
  - `PasswordEncoderConfig` — bean BCrypt.
  - `DataInitializer` — seed 1 admin, 2 rescuer, 3 victim và 20 `ReliefRequest` nếu bảng `users` trống.
- **controller** (6 class; không còn `RescuerController`)
  - `AuthController` — GET `/login` → `auth/login` (không dùng layout).
  - `HomeController` — GET `/home` redirect theo `ROLE_ADMIN` / `ROLE_RESCUER` / `ROLE_VICTIM`.
  - `VictimController` — `@RequestMapping("/victim")`, GET `/home`.
  - `AdminController` — `@RequestMapping("/admin")`, GET `/home`.
  - `RescuerRequestController` — `@RequestMapping("/rescuer")`, GET `/home` (placeholder trang chủ).
  - `RescuerTaskController` — `@RequestMapping("/rescuer")`, class rỗng, không method mapping.
- **model** — `User` (abstract, JOINED); `Admin` / `Victim` / `Rescuer`; `ReliefRequest`; enum `RequestStatus`, `NeedType`, `UrgencyLevel`.
- **repository** — `UserRepository` (`findByEmail`); subtype CRUD không query email; `ReliefRequestRepository` (`findByStatus|Victim|Rescuer`, `countByStatus`).
- **security** — `CustomUserDetailsService` load user bằng `UserRepository.findByEmail`, map `ROLE_ADMIN|RESCUER|VICTIM`.
- **test** — `ReliefCoordinationApplicationTests` (`contextLoads` only).

Chưa có package `service` / `dto`.

## 3. Template Thymeleaf (5 file)

Thư mục `src/main/resources/templates/`. CSS/JS: Bootstrap 5.3.3 CDN (jsDelivr), không Tabler.

| File | Vai trò |
|------|---------|
| `auth/login.html` | Form login độc lập, Bootstrap 5 CDN (chỉ CSS) |
| `fragments/layout.html` | Navbar trên (brand `/home`, `sec:authentication="name"`, logout CSRF); không sidebar; Bootstrap CSS + JS; `th:replace="~{${content} :: content}"` |
| `victim/home.html` | Fragment `content` trang chủ Victim |
| `rescuer/home.html` | Fragment `content` trang chủ Rescuer |
| `admin/home.html` | Fragment `content` trang chủ Admin |

Ba trang home nhận `content` và `pageTitle`; Victim / Admin / RescuerRequest còn gán `role`; layout không đọc thuộc tính đó.

## 4. Entity, quan hệ, kế thừa

**User (abstract)** — JOINED, bảng `users`: `id`, `fullName`, `email` (unique), `password`, `phoneNumber`, `enabled` (mặc định true).  
**Admin / Victim / Rescuer** — chỉ kế thừa User (bảng `admins` / `victims` / `rescuers`).

**ReliefRequest** — `id`; `@ManyToOne` bắt buộc `Victim`; `@ManyToOne` tùy chọn `Rescuer`; `location`; `latitude`/`longitude` (optional); `needType`; `numberOfPeople`; `urgencyLevel`; `description` (max 1000); `status` mặc định `SENT`; `createdAt`; `updatedAt`; `@Version`; `@PreUpdate` cập nhật `updatedAt`. Không `changeStatus`. Không `@OneToMany` ngược; không cascade/orphanRemoval. Lombok `@Setter` tạo `setStatus`.

## 5. Enum và chuyển trạng thái

- `NeedType`: FOOD, WATER, MEDICAL, SHELTER, RESCUE, OTHER.
- `UrgencyLevel`: LOW, MEDIUM, HIGH, CRITICAL.
- `RequestStatus`: SENT → ACCEPTED → PROCESSING → COMPLETED → CONFIRMED; CANCELLED từ SENT/ACCEPTED/PROCESSING.
  - SENT → ACCEPTED \| CANCELLED
  - ACCEPTED → PROCESSING \| CANCELLED
  - PROCESSING → COMPLETED \| CANCELLED
  - COMPLETED → CONFIRMED
  - CONFIRMED, CANCELLED: kết thúc.
- Chỉ có `canTransitionTo(next)` trên enum; không method đổi trạng thái chung. Không class nào gọi `canTransitionTo`.
- `setStatus` trực tiếp: chỉ `config/DataInitializer` khi seed 20 request (SENT / ACCEPTED / PROCESSING / COMPLETED / CONFIRMED).

## 6. Endpoint hiện có

Controller chỉ có `@GetMapping` (trừ `RescuerTaskController` không method). Không `@PostMapping` trong `controller/`. POST login/logout do Spring Security.

| URL | Method | Handler | Role |
|-----|--------|---------|------|
| `/login` | GET | `AuthController` | public |
| `/login` | POST | form login Spring | public |
| `/logout` | POST | Spring mặc định | đã đăng nhập |
| `/home` | GET | `HomeController` → redirect | authenticated |
| `/victim/home` | GET | `VictimController` | ROLE_VICTIM |
| `/rescuer/home` | GET | `RescuerRequestController` | ROLE_RESCUER |
| `/admin/home` | GET | `AdminController` | ROLE_ADMIN |

Public còn khai báo: `/`, `/register`, `/css/**`, `/js/**` (không có controller/view cho `/` hay `/register`).

## 7. Cấu hình Security

- Public: `/`, `/register`, `/login`, static `/css/**` `/js/**`; form login + logout `permitAll`.
- `hasRole` theo prefix `/victim/**`, `/rescuer/**`, `/admin/**`.
- Login page `/login`, thành công luôn `/home`; logout → `/login?logout`.
- CSRF bật; session form-login (không JWT).
- Auth: `DaoAuthenticationProvider` + BCrypt + `CustomUserDetailsService` (role theo `instanceof` subtype; `disabled` nếu `!enabled`).
- Lookup email: `UserRepository.findByEmail`.
- Layout (trừ login): `xmlns:sec` và `sec:authentication="name"` trên navbar; không sidebar nên không có link menu.

## 8. Đã xong / đang dở / rủi ro

**Xong:** model JOINED; enum + `canTransitionTo`; repository; BCrypt; SecurityFilterChain; seed; đăng nhập / đăng xuất CSRF; redirect `/home` theo role; layout Bootstrap (navbar, không sidebar); 3 trang home placeholder; Rescuer đã tách thành `RescuerRequestController` (home) và `RescuerTaskController` (rỗng).

**Dở:** không đăng ký; không CRUD yêu cầu; `RescuerTaskController` chưa có mapping; không package service nên chưa enforce `canTransitionTo` / ownership / `@Version`; seed gọi `setStatus` trực tiếp; chưa DTO/validation form.

**Rủi ro:** `ddl-auto=update` (không migration); test mới `contextLoads`.
