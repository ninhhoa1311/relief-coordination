# ARCHITECTURE — Kiến trúc hệ thống Cần cứu trợ

**Điều hướng:** [README](../README.md) · [SPEC](SPEC.md) · [OOP Design](OOP-DESIGN.md) · [Database](DATABASE.md) · [Workflow](WORKFLOW.md) · [Routes](ROUTES.md) · [User Guide](USER-GUIDE.md) · [Testing](TESTING.md)

---

## 1. Mô hình phân tầng

```
Browser / Client
      │
      ▼
┌─────────────────────┐
│   Controller Layer   │  Nhận HTTP request, validate form, trả về View name
│  (controller/)       │  Không chứa logic nghiệp vụ
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Service Layer      │  Logic nghiệp vụ, kiểm tra phân quyền sở hữu,
│  (service/)          │  gọi changeStatus(), xử lý exception
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Repository Layer    │  Spring Data JPA: truy vấn JPQL/derived query
│  (repository/)       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Model / Entity     │  @Entity JPA, business method (changeStatus, accept)
│  (model/)            │  Enum: RequestStatus, NeedType, UrgencyLevel
└─────────────────────┘
           │
           ▼
     PostgreSQL (Neon)
```

---

## 2. Vai trò từng package

| Package | Vai trò |
|---|---|
| `config/` | `SecurityConfig` (phân quyền URL), `PasswordEncoderConfig` (BCrypt bean), `DataInitializer` (seed dữ liệu mẫu) |
| `controller/` | Nhận HTTP, trả về tên template, không chứa logic nghiệp vụ. Mỗi role một controller. |
| `controller/form/` | Form object (`RegisterForm`, `ProfileForm`) dùng cho binding HTML form; có `@Valid`. |
| `dto/` | Data Transfer Object: `ReliefRequestForm`, `EmergencyRequestDto`, `PublicStatsDto` |
| `exception/` | `GlobalExceptionHandler` (`@ControllerAdvice`), `ResourceNotFoundException` |
| `model/` | Entity JPA và Enum. Chứa business method trên entity (`changeStatus`, `accept`, `isEditable`, `getMapUrl`). |
| `repository/` | Interface `JpaRepository`; custom JPQL query với `@Query` |
| `security/` | `CustomUserDetailsService`: load user theo email, gán role, kiểm tra `enabled` |
| `service/` | Logic nghiệp vụ, kiểm tra ownership, gọi repository |
| `util/` | Tiện ích dùng chung (nếu có) |

---

## 3. Danh sách Controller

| Controller | RequestMapping | Mô tả |
|---|---|---|
| `HomeController` | `/`, `/home` | Landing page (khách) và routing theo role |
| `AuthController` | `/login`, `/register/**` | Đăng nhập, đăng ký Victim/Rescuer |
| `EmergencyController` | `/emergency` | SOS không cần đăng nhập |
| `VictimController` | `/victim` | CRUD yêu cầu cứu trợ của Victim |
| `RescuerRequestController` | `/rescuer` | Danh sách, lọc, xem, nhận yêu cầu |
| `RescuerTaskController` | `/rescuer` | Nhiệm vụ của tôi, cập nhật trạng thái |
| `AdminController` | `/admin` | Dashboard, quản lý tài khoản và yêu cầu |
| `ProfileController` | `/profile` | Hồ sơ cá nhân (mọi role đã đăng nhập) |
| `ErrorPageController` | `/error` | Trang lỗi 403/404/500 |

---

## 4. Danh sách Service

| Service | Mô tả |
|---|---|
| `VictimRequestService` | Tạo, sửa, hủy, xem, xác nhận yêu cầu của Victim |
| `RescuerRequestService` | Danh sách SENT, lọc, phân trang, nhận nhiệm vụ |
| `RescuerTaskService` | Nhiệm vụ của Rescuer, cập nhật trạng thái tiếp theo |
| `AdminService` | Thống kê, quản lý tài khoản, duyệt Rescuer, hủy yêu cầu |
| `EmergencyService` | Tạo yêu cầu SOS, chống spam theo SĐT |
| `PublicStatsService` | Thống kê tổng hợp cho trang Landing (không lộ thông tin cá nhân) |
| `CurrentUserService` | Lấy User/Victim/Rescuer/Admin từ `Authentication` đang đăng nhập |

---

## 5. Luồng xử lý một request điển hình

Ví dụ: Victim gửi yêu cầu cứu trợ mới.

```
POST /victim/requests
        │
        ▼
VictimController.createRequest()
  ├── @Valid @ModelAttribute ReliefRequestForm → BindingResult
  ├── Nếu có lỗi → render lại form
  └── currentUserService.getCurrentUser(auth) → Victim
        │
        ▼
VictimRequestService.taoYeuCau(victim, form)
  ├── new ReliefRequest()
  ├── request.setVictim(victim)
  ├── applyForm(request, form) → set location, needTypes, numberOfPeople, urgencyLevel, description, GPS
  └── reliefRequestRepository.save(request)
        │
        ▼
ReliefRequestRepository (JPA) → INSERT INTO relief_requests + relief_request_need_types
        │
        ▼
redirect:/victim/home (kèm flash message thành công)
```

---

## 6. Cơ chế phân quyền Spring Security

### Phân quyền theo URL (SecurityConfig)

```java
.requestMatchers("/", "/login", "/register/**", "/emergency/**", "/error", "/css/**", "/js/**").permitAll()
.requestMatchers("/victim/**").hasRole("VICTIM")
.requestMatchers("/rescuer/**").hasRole("RESCUER")
.requestMatchers("/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

### Phân quyền theo ownership (Service)

`VictimRequestService.xemChiTiet()` kiểm tra `request.getVictim().getId().equals(victim.getId())`.

`RescuerTaskService.kiemTraQuyen()` kiểm tra `request.getRescuer().getId().equals(rescuer.getId())`.

### CustomUserDetailsService

Load user từ `UserRepository.findByEmail(email)`, gán role theo kiểu lớp con (`instanceof Victim` → `ROLE_VICTIM`, v.v.), và kiểm tra `user.isEnabled()` để chặn tài khoản bị khóa.

### Tài khoản bị khóa / chờ duyệt

Cả hai trường hợp (Rescuer chờ duyệt và tài khoản bị Admin khóa) đều lưu `enabled=false`. `AdminService` dùng `lockedRescuerIds` (ConcurrentHashMap in-memory) để phân biệt hai trạng thái này khi hiển thị trên giao diện.

> **Lưu ý:** `lockedRescuerIds` không persistent — restart app sẽ mất thông tin phân biệt giữa "chờ duyệt" và "bị khóa" cho Rescuer `enabled=false`. Đây là thiết kế hiện tại.

---

## 7. Xử lý ngoại lệ

`GlobalExceptionHandler` (`@ControllerAdvice`) xử lý:

| Exception | Hành động |
|---|---|
| `IllegalStateException` | Redirect về trang trước (Referer) kèm flash message lỗi |
| `ObjectOptimisticLockingFailureException` | Redirect về trang trước kèm thông báo "Yêu cầu đã được người khác xử lý" |
| `AccessDeniedException` | Render trang 403 |
| `ResourceNotFoundException` | Render trang 404 |

Trang lỗi: `error/403.html`, `error/404.html`, `error/500.html`, `error/error.html` — đều dùng `fragments/layout.html`.

---

## 8. Tổ chức Template Thymeleaf

### Layout chung

`fragments/layout.html` là khung dùng cho tất cả trang **đã đăng nhập**:
- Nhúng nội dung qua `th:replace="~{${content} :: content}"` — Controller truyền tên template vào `model.addAttribute("content", "victim/home")`.
- Menu theo role: nhúng `nav-victim.html`, `nav-rescuer.html`, hoặc `nav-admin.html` bằng `sec:authorize`.
- Navbar hiển thị tên người dùng (`sec:authentication="name"`), nút đăng xuất có CSRF token.

### Fragment dùng chung

| Fragment | File | Công dụng |
|---|---|---|
| `status-badge` | `fragments/status-badge.html` | Badge màu theo `RequestStatus` |
| `urgency-badge` | `fragments/urgency-badge.html` | Badge màu theo `UrgencyLevel` |
| `need-badges` | `fragments/need-badges.html` | Danh sách badge loại nhu cầu |
| `urgency-picker` | `fragments/urgency-picker.html` | Bộ chọn mức khẩn cấp (4 nút radio) |
| `progress-steps` | `fragments/progress-steps.html` | Thanh tiến trình trạng thái yêu cầu |
| `back-home` | `fragments/back-home.html` | Nút "Quay lại" |

### Trang công khai (độc lập, không dùng layout)

`landing.html`, `auth/login.html`, `auth/register-victim.html`, `auth/register-rescuer.html`, `emergency/form.html`, `emergency/success.html`.

---

## 9. Hệ thống thiết kế giao diện (app.css)

File `src/main/resources/static/css/app.css` là design system dùng chung:

| CSS Variable / Class | Giá trị / Mô tả |
|---|---|
| `--color-navy` | `#0f1b2d` — màu chính navbar và text |
| `--color-red` | `#c8102e` — màu nhấn (nút SOS, logo) |
| `--color-green` | `#16a34a` — màu thành công |
| `--color-blue` | `#2563eb` — màu thông tin / link |
| `--bg-page` | `#f5f1ea` — nền trang |
| `--font-main` | `Be Vietnam Pro` (Google Fonts CDN) |
| `.app-navbar` | Navbar chung (nền navy) |
| `.app-card` | Card giao diện |

Không thêm CSS inline trong template; dùng class từ `app.css` hoặc Bootstrap 5.
