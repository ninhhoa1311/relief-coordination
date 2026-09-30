# Relief Coordination — kiến trúc hiện tại

Ứng dụng Spring Boot điều phối cứu trợ: nạn nhân gửi yêu cầu, cứu hộ nhận/xử lý, admin quản trị. Tầng web (controller, service, Thymeleaf) **chưa có**; mới có domain, JPA, security skeleton và seed data.

## 1. Phiên bản và dependency

- **Java 21**, **Spring Boot 4.1.1**, Maven, artifact `relief-coordination` `0.0.1-SNAPSHOT`.
- Chính: `spring-boot-starter-webmvc`, `data-jpa`, `security`, `thymeleaf`, `validation`.
- Khác: `thymeleaf-extras-springsecurity6`, PostgreSQL (runtime), Lombok, DevTools (optional).
- Test: các starter `*-test` tương ứng (JPA, Security, Thymeleaf, Validation, WebMVC).
- Cấu hình: port 8080; datasource từ biến môi trường (không hard-code); JPA `ddl-auto=update`, `open-in-view=false`; Thymeleaf cache tắt.

## 2. Cây package và vai trò class

Gốc: `com.reliefcoordination.relief_coordination`

- `ReliefCoordinationApplication` — bootstrap Spring Boot.
- `DataInitializer` — seed admin/rescuer/victim + 20 `ReliefRequest` nếu `users` trống.
- **model**
  - `User` — entity trừu tượng, JOINED, bảng `users`.
  - `Admin` / `Victim` / `Rescuer` — subtype (bảng `admins` / `victims` / `rescuers`), không field thêm.
  - `ReliefRequest` — yêu cầu cứu trợ, bảng `relief_requests`.
  - `RequestStatus` / `NeedType` / `UrgencyLevel` — enum trạng thái / loại nhu cầu / mức khẩn.
- **repository**
  - `UserRepository` — JPA `User`; `findByEmail`.
  - `AdminRepository` / `VictimRepository` / `RescuerRepository` — CRUD subtype, **không** query theo email.
  - `ReliefRequestRepository` — `findByStatus|Victim|Rescuer`, `countByStatus`.
- **config**
  - `SecurityConfig` — filter chain, form login, phân quyền theo prefix URL.
  - `PasswordEncoderConfig` — bean BCrypt.
- **security**
  - `CustomUserDetailsService` — load user theo email, map `ROLE_ADMIN|RESCUER|VICTIM`.
- **test**
  - `ReliefCoordinationApplicationTests` — chỉ `contextLoads`.

Chưa có package `controller`, `service`, `dto`; chưa có template/static.

## 3. Entity, quan hệ, kế thừa

**User (abstract)** — JOINED: `id`, `fullName`, `email` (unique), `password`, `phoneNumber`, `enabled` (mặc định true).  
**Admin / Victim / Rescuer** — chỉ kế thừa User, không quan hệ mapped phía user.

**ReliefRequest**

- `id`; `@ManyToOne` bắt buộc `Victim` (`victim_id`); `@ManyToOne` tùy chọn `Rescuer` (`rescuer_id`).
- `location`; `latitude`/`longitude` (optional); `needType`; `numberOfPeople`; `urgencyLevel`; `description` (max 1000).
- `status` mặc định `SENT`; `createdAt` (không update); `updatedAt` (`@PreUpdate`); `@Version` optimistic lock.
- **Không** có `@OneToMany` ngược trên Victim/Rescuer; không cascade/orphanRemoval.

## 4. Enum và chuyển trạng thái

- `NeedType`: FOOD, WATER, MEDICAL, SHELTER, RESCUE, OTHER.
- `UrgencyLevel`: LOW, MEDIUM, HIGH, CRITICAL.
- `RequestStatus`: SENT → ACCEPTED → PROCESSING → COMPLETED → CONFIRMED; **CANCELLED** từ SENT/ACCEPTED/PROCESSING.
  - SENT → ACCEPTED | CANCELLED  
  - ACCEPTED → PROCESSING | CANCELLED  
  - PROCESSING → COMPLETED | CANCELLED  
  - COMPLETED → CONFIRMED  
  - CONFIRMED, CANCELLED: kết thúc.
- `canTransitionTo(next)` kiểm tra map trên; **chưa** gọi từ service/controller.

## 5. Endpoint hiện có

**Không có** `@Controller` / `@RestController`. URL dưới đây chỉ nằm trong `SecurityConfig` (chưa handler → 404 nếu gọi):

| URL | Method dự kiến | Role |
|-----|----------------|------|
| `/`, `/register`, `/login`, `/css/**`, `/js/**` | GET (login POST form mặc định) | public |
| `/login` POST | form login Spring | public |
| logout (mặc định `/logout` POST) | POST | đã đăng nhập |
| `/home` | sau login (`defaultSuccessUrl`) | authenticated |
| `/victim/**` | — | ROLE_VICTIM |
| `/rescuer/**` | — | ROLE_RESCUER |
| `/admin/**` | — | ROLE_ADMIN |
| còn lại | — | authenticated |

## 6. Cấu hình Security

- Public: `/`, `/register`, `/login`, static `/css/**` `/js/**`; form login + logout `permitAll`.
- `hasRole("VICTIM"|"RESCUER"|"ADMIN")` cho prefix tương ứng (Spring thêm `ROLE_`).
- Login page `/login`, thành công luôn `/home`; logout → `/login?logout`.
- CSRF mặc định **bật**; session form-login (không JWT).
- Auth: `DaoAuthenticationProvider` + BCrypt + `CustomUserDetailsService` (role theo `instanceof` subtype; `disabled` nếu `!enabled`).
- Lookup email: `findAll()` rồi filter trên Admin → Rescuer → Victim; **không** dùng `UserRepository.findByEmail`.

## 7. Đã xong / đang dở / còn lỗi

**Xong:** model + JOINED; enum + transition map; repository; BCrypt tách bean; SecurityFilterChain; seed (1 admin, 2 rescuer, 3 victim, 20 request các status); properties env; test context.

**Dở:** không UI login/register/home; không CRUD yêu cầu; không enforce `canTransitionTo` / gán rescuer; không DTO/validation form; subtype repo thiếu `findByEmail`; seed mật khẩu plaintext trong code (chỉ hash khi lưu).

**Lỗi / rủi ro:** login xong `/home` 404; `/login` `/register` không view; `CustomUserDetailsService` load toàn bảng (chậm, race nếu email trùng subtype); `UserRepository.findByEmail` thừa; không `@OneToMany`; không service nên optimistic lock/`canTransitionTo` chưa dùng; `ddl-auto=update` không thay migration; test chưa cover security/transition.
