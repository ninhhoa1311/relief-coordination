# Luật dự án — Cần cứu trợ

Đọc chi tiết trong `.cursor/rules/project.mdc` và `docs/SPEC.md` trước khi làm bất kỳ việc gì — hai file đó là nguồn luật chính của dự án, áp dụng cho mọi công cụ AI (Cursor, Antigravity, v.v.).

## Tóm tắt nhanh

- **Tên sản phẩm:** Cần cứu trợ. Tên kỹ thuật (Maven artifactId): `relief-coordination`.
- **Stack:** Spring Boot 4.1.1, Java 21, Maven, Spring Security 7, Spring Data JPA, Thymeleaf, PostgreSQL (Neon), Bootstrap 5 CDN.
- **Kiến trúc:** controller → service → repository → model. Không đặt logic nghiệp vụ trong Controller.
- **Mỗi role một controller:** `VictimController`, `RescuerRequestController`, `RescuerTaskController`, `AdminController`. Dùng `@RequestMapping("/role")` ở mức class.
- **Chuyển trạng thái ReliefRequest** CHỈ qua `request.changeStatus(...)` hoặc `request.accept(rescuer)`. KHÔNG bao giờ `setStatus(...)` trực tiếp (trừ `DataInitializer` khi seed).
- **Phân quyền:** kiểm tra cả role (SecurityConfig) và quyền sở hữu dữ liệu (Service). Không chỉ dựa vào URL.
- **Không đọc, ghi hay commit file `.env`.** Không hard-code mật khẩu hay connection string.
- **Không thêm dependency vào `pom.xml`**, không sửa entity trong `model/`, `SecurityConfig`, `layout.html` khi chưa hỏi nhóm trưởng.
- **Test** dùng `@Transactional` để tự rollback, không thêm H2 hay dependency test mới.
- Sau mỗi thay đổi code, chạy `.\mvnw.cmd -q compile` và báo kết quả.

## Cấu trúc package quan trọng

```
com.reliefcoordination.relief_coordination
├── config/        SecurityConfig, PasswordEncoderConfig, DataInitializer
├── controller/    AuthController, HomeController, EmergencyController, VictimController,
│                  RescuerRequestController, RescuerTaskController, AdminController,
│                  ProfileController, ErrorPageController
│   └── form/      RegisterForm, ProfileForm
├── dto/           ReliefRequestForm, EmergencyRequestDto, PublicStatsDto
├── exception/     GlobalExceptionHandler, ResourceNotFoundException
├── model/         User (abstract), Victim, Rescuer, Admin, ReliefRequest
│                  RequestStatus, NeedType, UrgencyLevel
├── repository/    UserRepository, VictimRepository, RescuerRepository, AdminRepository,
│                  ReliefRequestRepository
├── security/      CustomUserDetailsService
└── service/       VictimRequestService, RescuerRequestService, RescuerTaskService,
                   AdminService, EmergencyService, PublicStatsService, CurrentUserService
```

## Quy ước giao diện

- Trang đã đăng nhập dùng `fragments/layout.html`; trang con bọc nội dung trong `<div th:fragment="content">`.
- Trang công khai (landing, auth, emergency) là trang độc lập, không dùng layout.
- Menu theo role: chỉ sửa file `fragments/nav-<role>.html` của role mình. Không sửa `layout.html`.
- Màu badge trạng thái: SENT (xám), ACCEPTED (xanh dương), PROCESSING (vàng), COMPLETED (xanh lá nhạt), CONFIRMED (xanh lá đậm), CANCELLED (đỏ).
- Fragment dùng chung: `status-badge`, `urgency-badge`, `need-badges`, `urgency-picker`, `progress-steps`, `back-home`.
- Design system CSS: file `static/css/app.css`. Không inline CSS trong template.

## Migration và database

- Mỗi thành viên dùng một Neon branch riêng.
- File migration SQL cần chạy tay trên mỗi Neon branch (xem `docs/migration-need-types.sql`).
- Seed lại: `TRUNCATE TABLE users RESTART IDENTITY CASCADE;` trên Neon branch của mình.
