# WORKFLOW — Luồng nghiệp vụ Cần cứu trợ

**Điều hướng:** [README](../README.md) · [SPEC](SPEC.md) · [Kiến trúc](ARCHITECTURE.md) · [OOP Design](OOP-DESIGN.md) · [Database](DATABASE.md) · [Routes](ROUTES.md) · [User Guide](USER-GUIDE.md) · [Testing](TESTING.md)

---

## 1. Sơ đồ trạng thái yêu cầu cứu trợ

```mermaid
stateDiagram-v2
    [*] --> SENT : Victim tạo yêu cầu\nhoặc Khách gửi SOS

    SENT --> ACCEPTED : Rescuer nhận nhiệm vụ
    SENT --> CANCELLED : Victim hủy\nhoặc Admin hủy

    ACCEPTED --> PROCESSING : Rescuer bắt đầu xử lý
    ACCEPTED --> CANCELLED : Chỉ Admin

    PROCESSING --> COMPLETED : Rescuer đánh dấu hoàn thành
    PROCESSING --> CANCELLED : Chỉ Admin

    COMPLETED --> CONFIRMED : Victim xác nhận nhận hỗ trợ
    COMPLETED --> CANCELLED : Chỉ Admin

    CONFIRMED --> [*]
    CANCELLED --> [*]
```

**Ghi chú:**
- Trạng thái `CONFIRMED` và `CANCELLED` là trạng thái cuối — không chuyển tiếp.
- Mọi chuyển trạng thái đều qua `request.changeStatus()` → `RequestStatus.canTransitionTo()`.
- `request.accept(rescuer)` gọi `changeStatus(ACCEPTED)` trước rồi gán `rescuer`.

---

## 2. Luồng (a): Victim gửi yêu cầu → Rescuer xử lý → Victim xác nhận

```mermaid
sequenceDiagram
    actor Victim
    actor Rescuer
    participant System as Cần cứu trợ

    Victim->>System: Đăng nhập (/login)
    System-->>Victim: Chuyển đến /victim/home

    Victim->>System: GET /victim/requests/new
    System-->>Victim: Hiển thị form tạo yêu cầu

    Victim->>System: POST /victim/requests (location, needTypes, numberOfPeople, urgencyLevel, GPS?)
    System-->>Victim: Tạo yêu cầu status=SENT, redirect /victim/home

    Note over Rescuer: Đã đăng nhập, tài khoản đã được Admin duyệt

    Rescuer->>System: GET /rescuer/home (danh sách SENT)
    System-->>Rescuer: Danh sách yêu cầu đang chờ (khẩn cấp lên đầu)

    Rescuer->>System: GET /rescuer/requests/{id} (xem chi tiết, chưa thấy tên/SĐT Victim)
    System-->>Rescuer: Chi tiết yêu cầu (ẩn thông tin cá nhân Victim)

    Rescuer->>System: POST /rescuer/requests/{id}/accept
    System-->>Rescuer: Chuyển SENT→ACCEPTED, gán rescuer, redirect /rescuer/home

    Rescuer->>System: GET /rescuer/tasks/{id}
    System-->>Rescuer: Chi tiết nhiệm vụ (hiển thị tên + SĐT Victim)

    Rescuer->>System: POST /rescuer/tasks/{id}/status (ACCEPTED→PROCESSING)
    System-->>Rescuer: Cập nhật trạng thái, flash message

    Rescuer->>System: POST /rescuer/tasks/{id}/status (PROCESSING→COMPLETED)
    System-->>Rescuer: Cập nhật trạng thái, flash message

    Victim->>System: GET /victim/requests/{id}
    System-->>Victim: Trạng thái COMPLETED, nút "Xác nhận đã nhận hỗ trợ"

    Victim->>System: POST /victim/requests/{id}/confirm
    System-->>Victim: Chuyển COMPLETED→CONFIRMED, flash message thành công
```

---

## 3. Luồng (b): Khách gửi SOS không cần tài khoản → Rescuer thấy ngay

```mermaid
sequenceDiagram
    actor Guest as Khách (không có tài khoản)
    actor Rescuer
    participant System as Cần cứu trợ

    Guest->>System: GET /emergency (trang SOS)
    System-->>Guest: Form nhập tên + SĐT + GPS tùy chọn

    Guest->>System: POST /emergency (fullName, phoneNumber, latitude?, longitude?)
    Note over System: EmergencyService.taoYeuCauSOS():\n- victim = null\n- guestName = fullName\n- guestPhone = phoneNumber\n- urgencyLevel = CRITICAL\n- needType = RESCUE\n- status = SENT

    alt Cùng SĐT đã có yêu cầu đang hoạt động
        System-->>Guest: redirect /emergency/success + thông báo "Đã ghi nhận trước đó"
    else SĐT mới
        System->>System: Lưu yêu cầu SOS vào DB
        System-->>Guest: redirect /emergency/success + thông báo thành công
    end

    Note over Rescuer: Đăng nhập, vào /rescuer/home

    Rescuer->>System: GET /rescuer/home
    System-->>Rescuer: Danh sách SENT — yêu cầu SOS (CRITICAL) nổi lên đầu

    Rescuer->>System: GET /rescuer/requests/{id} (yêu cầu SOS)
    System-->>Rescuer: Chi tiết: location="Tọa độ GPS" hoặc "Chưa xác định", guestName, guestPhone hiển thị

    Rescuer->>System: POST /rescuer/requests/{id}/accept
    System-->>Rescuer: Chuyển SENT→ACCEPTED, redirect /rescuer/home
```

---

## 4. Luồng duyệt Rescuer (Admin)

```mermaid
sequenceDiagram
    actor Rescuer
    actor Admin
    participant System as Cần cứu trợ

    Rescuer->>System: POST /register/rescuer (fullName, email, phoneNumber, password)
    System->>System: Tạo tài khoản Rescuer với enabled=false
    System-->>Rescuer: redirect /login + flash "Tài khoản chờ Admin xác minh"

    Rescuer->>System: POST /login
    System-->>Rescuer: Lỗi: tài khoản bị khóa (chưa duyệt)

    Admin->>System: GET /admin/users?tab=rescuer
    System-->>Admin: Danh sách Rescuer, thấy rescuer mới ở trạng thái "Chờ duyệt"

    Admin->>System: POST /admin/rescuers/{id}/approve
    System->>System: rescuer.setEnabled(true)
    System-->>Admin: redirect /admin/users?tab=rescuer + flash "Đã duyệt"

    Rescuer->>System: POST /login (thử lại)
    System-->>Rescuer: Đăng nhập thành công, chuyển đến /rescuer/home
```

