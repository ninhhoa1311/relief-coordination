# SPEC — Cần cứu trợ — Đặc tả yêu cầu hệ thống

> Nguồn luật chính của dự án. Mọi thành viên và AI agent đọc trước khi sửa code.
> Khi có mâu thuẫn, mục **0. Quyết định đã chốt** được ưu tiên.

**Điều hướng:** [README](../README.md) · [Kiến trúc](ARCHITECTURE.md) · [OOP Design](OOP-DESIGN.md) · [Database](DATABASE.md) · [Workflow](WORKFLOW.md) · [Routes](ROUTES.md) · [User Guide](USER-GUIDE.md) · [Testing](TESTING.md)

---

## 0. Quyết định đã chốt

**Phạm vi — KHÔNG làm:**
- Xác thực email / OTP, quên mật khẩu.
- WebSocket / real-time, upload file / ảnh.
- Biểu đồ (chỉ dùng thẻ số).
- Nhúng bản đồ (chỉ dùng link Google Maps `https://www.google.com/maps?q={lat},{lng}`).
- Quản lý kho vật tư, thanh toán.

**Kỹ thuật đã chốt:**
- Stack: Spring Boot 4.1.1, Java 21, Maven, Spring Security 7, Spring Data JPA/Hibernate, Thymeleaf.
- Giao diện: Bootstrap 5 CDN, Bootstrap Icons CDN, Google Fonts Be Vietnam Pro CDN, không dùng template admin có sẵn.
- Database: PostgreSQL trên Neon. Mỗi thành viên một Neon branch riêng; thông tin kết nối trong `.env` (git-ignored).
- `User` là abstract class, kế thừa chiến lược `JOINED`, 3 lớp con: `Victim` / `Rescuer` / `Admin`.
- Trạng thái yêu cầu: `SENT → ACCEPTED → PROCESSING → COMPLETED → CONFIRMED`, thêm `CANCELLED`. Chuyển trạng thái CHỈ qua `request.changeStatus()` / `request.accept()`.
- Chống nhận trùng: `@Version` (optimistic locking) trên `ReliefRequest`.
- Rescuer tách thành 2 controller: `RescuerRequestController` (danh sách, lọc, nhận) và `RescuerTaskController` (nhiệm vụ của tôi, cập nhật trạng thái).

**Tài khoản:**
- Victim tự đăng ký, `enabled=true` ngay lập tức.
- Rescuer tự đăng ký, `enabled=false`; phải chờ Admin duyệt.
- Admin chỉ được tạo qua `DataInitializer` (không có trang đăng ký Admin).
- Admin khóa/mở khóa Victim và Rescuer; không thể khóa tài khoản Admin.

---

## 1. Giới thiệu

**Mục tiêu:** Kết nối người cần cứu trợ (Victim) với người cứu trợ (Rescuer) khi có thiên tai (bão, lũ, sạt lở), quản lý toàn bộ vòng đời yêu cầu cứu trợ từ lúc gửi đến khi xác nhận hoàn thành.

**Phạm vi:** Điều phối yêu cầu; phù hợp nhóm nhỏ (5 thành viên). Không bao gồm các tính năng liệt kê trong mục 0.

---

## 2. Vai trò và chức năng

### 2.1. Khách chưa đăng nhập
- Xem trang Landing (`/`): số liệu thống kê tổng hợp (tổng yêu cầu, đã hoàn thành, người được hỗ trợ).
- Gửi **yêu cầu SOS khẩn cấp** (`/emergency`): nhập tên, số điện thoại; tùy chọn chia sẻ GPS. Không cần tài khoản.
- Đăng ký Victim (`/register/victim`) hoặc Rescuer (`/register/rescuer`).
- Đăng nhập (`/login`).

### 2.2. Victim
- Đăng ký (họ tên, email, SĐT, mật khẩu), đăng nhập, đăng xuất.
- Tạo yêu cầu cứu trợ:
  - Địa điểm (text, bắt buộc).
  - Loại nhu cầu (chọn nhiều): Thực phẩm / Nước uống / Y tế / Nơi ở / Cứu người / Khác — bắt buộc ít nhất 1.
  - Số người cần hỗ trợ (bắt buộc, ≥ 1).
  - Mức độ khẩn cấp (bắt buộc): Thấp / Trung bình / Cao / Khẩn cấp. Mặc định Trung bình.
  - Mô tả (tùy chọn, tối đa 1000 ký tự).
  - Vị trí GPS (tùy chọn): lấy bằng Geolocation API trình duyệt, **một lần khi bấm nút**. Từ chối quyền vị trí vẫn gửi được yêu cầu.
- Xem danh sách và chi tiết yêu cầu **của mình**.
- Sửa và hủy yêu cầu **chỉ khi còn trạng thái `SENT`**.
- Sau khi yêu cầu được Rescuer nhận: xem tên và SĐT của Rescuer phụ trách.
- Khi yêu cầu `COMPLETED`: bấm **Xác nhận đã nhận hỗ trợ** → chuyển sang `CONFIRMED`.
- Cập nhật hồ sơ cá nhân (họ tên, SĐT, đổi mật khẩu).

### 2.3. Rescuer
- Đăng ký, chờ Admin duyệt, đăng nhập, đăng xuất.
- Xem danh sách yêu cầu `SENT`; lọc theo địa điểm (tìm kiếm text) / loại nhu cầu / mức khẩn cấp; sắp xếp theo thời gian tạo (mới nhất / cũ nhất trước); phân trang 10/trang.
- Xem chi tiết yêu cầu **trước khi nhận**: xem mã, địa điểm, link bản đồ, loại nhu cầu, số người, mức khẩn cấp, mô tả, thời gian tạo. **Không** thấy họ tên và SĐT Victim.
- Nhận nhiệm vụ (được nhận nhiều yêu cầu cùng lúc).
- **Nhiệm vụ của tôi** (`/rescuer/tasks`): xem, lọc, sắp xếp nhiệm vụ đã nhận; cập nhật trạng thái `ACCEPTED → PROCESSING → COMPLETED`.
- Sau khi nhận: xem được họ tên và SĐT Victim.
- Xem thống kê cá nhân: đang làm / hoàn thành / tổng.
- Cập nhật hồ sơ cá nhân.

### 2.4. Admin
- Xem dashboard: **8 thẻ số** thống kê toàn hệ thống (Tổng yêu cầu / Đã gửi / Đã tiếp nhận / Đang xử lý / Hoàn thành / Đã xác nhận / Đã hủy / Số người được hỗ trợ).
- Xem và lọc toàn bộ yêu cầu (status, urgencyLevel, needType, từ khóa); phân trang; yêu cầu chưa xử lý khẩn cấp nổi lên đầu.
- Hủy bất kỳ yêu cầu nào ngoại trừ đã `CONFIRMED` hoặc đã `CANCELLED`.
- Xem danh sách Victim và Rescuer; khóa / mở khóa tài khoản (không khóa Admin).
- Duyệt Rescuer mới đăng ký: từng người hoặc duyệt tất cả.
- Xem chi tiết một Victim: thông tin cá nhân + lịch sử yêu cầu có phân trang + thống kê.
- **Không** trực tiếp xử lý yêu cầu cứu trợ.

---

## 3. Vòng đời yêu cầu cứu trợ

```
SENT → ACCEPTED → PROCESSING → COMPLETED → CONFIRMED
 ↓         ↓           ↓            ↓
CANCELLED  CANCELLED  CANCELLED   CANCELLED (chỉ Admin)
```

### Quy tắc chuyển trạng thái

| Từ | Sang | Ai thực hiện |
|---|---|---|
| `SENT` | `ACCEPTED` | Rescuer bất kỳ (nhận yêu cầu, gán rescuer) |
| `SENT` | `CANCELLED` | Victim chủ yêu cầu **hoặc** Admin |
| `ACCEPTED` | `PROCESSING` | Rescuer đã nhận yêu cầu đó |
| `ACCEPTED` | `CANCELLED` | Chỉ Admin |
| `PROCESSING` | `COMPLETED` | Rescuer đã nhận yêu cầu đó |
| `PROCESSING` | `CANCELLED` | Chỉ Admin |
| `COMPLETED` | `CONFIRMED` | Victim chủ yêu cầu |
| `COMPLETED` | `CANCELLED` | Chỉ Admin |

### Quy tắc bổ sung
- `CONFIRMED` và `CANCELLED` là **trạng thái cuối**, không chuyển tiếp.
- Chuyển trạng thái CHỈ qua `request.changeStatus(next)` hoặc `request.accept(rescuer)`.
- Kiểm tra người thực hiện ở tầng **Service** (không chỉ ở URL).
- Nếu hai Rescuer cùng nhận một yêu cầu, `@Version` làm một bên thất bại; `GlobalExceptionHandler` bắt `ObjectOptimisticLockingFailureException` và redirect kèm thông báo thân thiện.

---

## 4. Tính năng SOS khẩn cấp

- URL: `GET /emergency` (form), `POST /emergency` (submit), `GET /emergency/success` (xác nhận).
- Không yêu cầu đăng nhập.
- Chỉ cần: họ tên + số điện thoại. GPS tùy chọn.
- Yêu cầu SOS tự động: `urgencyLevel = CRITICAL`, `needType = {RESCUE}`, `victim = null`, `guestName` + `guestPhone` thay thế.
- Chống spam: nếu cùng số điện thoại đã có yêu cầu đang hoạt động (chưa CONFIRMED/CANCELLED), không tạo mới — hiển thị thông báo nhắc liên hệ lại.

---

## 5. Quyền riêng tư và phân quyền dữ liệu

- Victim chỉ thấy, sửa, hủy, xác nhận **yêu cầu của mình**; kiểm tra trong `VictimRequestService`.
- Rescuer chỉ cập nhật trạng thái **yêu cầu mình đã nhận**; kiểm tra trong `RescuerTaskService`.
- Trước khi nhận: Rescuer **không thấy** họ tên và SĐT Victim (ẩn ở tầng template).
- Sau khi nhận: Rescuer thấy họ tên và SĐT Victim.
- Victim thấy họ tên và SĐT Rescuer **sau khi yêu cầu được nhận**.
- Mật khẩu luôn mã hóa BCrypt.

---

## 6. Yêu cầu phi chức năng

- **Bảo mật:** Spring Security 7, BCrypt, CSRF bật, phân quyền cả ở URL (SecurityConfig) và ở Service (ownership check).
- **Tài khoản bị khóa:** `enabled=false` → `CustomUserDetailsService` ném `LockedException` → Spring Security trả về lỗi đăng nhập.
- **Optimistic locking:** `@Version` trên `ReliefRequest` bảo vệ trường hợp 2 Rescuer cùng nhận 1 yêu cầu.
- **Responsive:** Bootstrap 5 — giao diện co giãn trên mobile/tablet/desktop.
- **Phân trang:** danh sách yêu cầu (Admin, Rescuer) 10 kết quả/trang.
- **Xử lý lỗi:** `GlobalExceptionHandler` bắt `IllegalStateException`, `ObjectOptimisticLockingFailureException`, `AccessDeniedException`, `ResourceNotFoundException`; trang lỗi 403/404/500 có giao diện.

---

## 7. Thống kê Admin (8 thẻ số)

| Thẻ | Nội dung |
|---|---|
| Tổng số yêu cầu | Tất cả yêu cầu trong hệ thống |
| Đã gửi (SENT) | Đang chờ Rescuer nhận |
| Đã tiếp nhận (ACCEPTED) | Rescuer đã nhận, chưa xử lý |
| Đang xử lý (PROCESSING) | Đang được xử lý |
| Hoàn thành (COMPLETED) | Xử lý xong, chờ Victim xác nhận |
| Đã xác nhận (CONFIRMED) | Victim đã xác nhận nhận được hỗ trợ |
| Đã hủy (CANCELLED) | Đã bị hủy |
| Người được hỗ trợ | Tổng `numberOfPeople` của các yêu cầu `CONFIRMED` |

---

## 8. Giao diện

- Trang của người đã đăng nhập dùng `fragments/layout.html` qua `th:replace="~{${content} :: content}"`. Trang con bọc nội dung trong `<div th:fragment="content">`.
- Trang công khai (`landing.html`, `auth/login.html`, `auth/register-*.html`, `emergency/form.html`) là **trang độc lập**, không dùng layout.
- Menu theo role trong 3 file riêng: `fragments/nav-victim.html`, `fragments/nav-rescuer.html`, `fragments/nav-admin.html`. Không sửa `layout.html`.
- Màu badge trạng thái thống nhất: SENT (xám) / ACCEPTED (xanh dương) / PROCESSING (vàng) / COMPLETED (xanh lá nhạt) / CONFIRMED (xanh lá đậm) / CANCELLED (đỏ).
- Fragment dùng chung: `status-badge`, `urgency-badge`, `need-badges`, `urgency-picker`, `progress-steps`, `back-home`.

---

## 9. Quy ước kỹ thuật

- **Trạng thái:** dùng `request.changeStatus(next)`; KHÔNG gọi `request.setStatus(...)` trong Service/Controller.
- **Nhận yêu cầu:** dùng `request.accept(rescuer)` — kiểm tra bước đi trước, gán rescuer sau.
- **Form/DTO:** không bind thẳng entity vào form; dùng `ReliefRequestForm`, `EmergencyRequestDto`, `RegisterForm`, `ProfileForm` có `@Valid`.
- **Repository:** thêm method mới vào cuối `ReliefRequestRepository`, không sửa/xóa method của người khác.
- **File được bảo vệ:** `pom.xml`, `SecurityConfig`, `application.properties`, `model/`, `layout.html`, `DataInitializer` — chỉ nhóm trưởng sửa.

---

## 10. Dữ liệu mẫu (DataInitializer)

Chạy khi bảng `users` trống. Tạo:
- 1 Admin: `admin@relief.vn` / `admin123`
- 2 Rescuer: `rescuer1@relief.vn`, `rescuer2@relief.vn` / `rescuer123`
- 3 Victim: `victim1@relief.vn`, `victim2@relief.vn`, `victim3@relief.vn` / `victim123`
- 20 yêu cầu mẫu: SENT ×7, ACCEPTED ×3, PROCESSING ×3, COMPLETED ×2, CONFIRMED ×3, CANCELLED ×2. Yêu cầu số chẵn có GPS.
- 2 yêu cầu SOS mẫu (victim = null): 1 không có GPS, 1 có GPS (tọa độ Đà Nẵng).

Muốn seed lại: `TRUNCATE TABLE users RESTART IDENTITY CASCADE;` trên Neon branch của mình, rồi khởi động lại app.
