# ROUTES — Danh sách toàn bộ route

**Điều hướng:** [README](../README.md) · [SPEC](SPEC.md) · [Kiến trúc](ARCHITECTURE.md) · [OOP Design](OOP-DESIGN.md) · [Database](DATABASE.md) · [Workflow](WORKFLOW.md) · [User Guide](USER-GUIDE.md) · [Testing](TESTING.md)

---

## Trang công khai (không cần đăng nhập)

| Phương thức | Đường dẫn | Controller | Mô tả |
|---|---|---|---|
| GET | `/` | `HomeController` | Landing page; người đã đăng nhập → redirect `/home` |
| GET | `/login` | `AuthController` | Trang đăng nhập |
| POST | `/login` | Spring Security | Xử lý đăng nhập |
| POST | `/logout` | Spring Security | Đăng xuất (CSRF required) |
| GET | `/register/victim` | `AuthController` | Form đăng ký Victim |
| POST | `/register/victim` | `AuthController` | Xử lý đăng ký Victim |
| GET | `/register/rescuer` | `AuthController` | Form đăng ký Rescuer |
| POST | `/register/rescuer` | `AuthController` | Xử lý đăng ký Rescuer |
| GET | `/emergency` | `EmergencyController` | Form SOS khẩn cấp |
| POST | `/emergency` | `EmergencyController` | Gửi yêu cầu SOS |
| GET | `/emergency/success` | `EmergencyController` | Trang xác nhận SOS |
| GET | `/error` | `ErrorPageController` | Trang lỗi (403/404/500) |
| GET | `/css/**` | Static | CSS |
| GET | `/js/**` | Static | JavaScript |

---

## Điểm vào chung (cần đăng nhập)

| Phương thức | Đường dẫn | Controller | Mô tả |
|---|---|---|---|
| GET | `/home` | `HomeController` | Routing theo role → `/victim/home`, `/rescuer/home`, `/admin/home` |
| GET | `/profile` | `ProfileController` | Xem / sửa hồ sơ cá nhân |
| POST | `/profile` | `ProfileController` | Cập nhật hồ sơ (họ tên, SĐT, đổi mật khẩu) |

---

## Victim (`/victim/**` — Role: VICTIM)

| Phương thức | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/victim/home` | Dashboard: danh sách yêu cầu của tôi + thống kê |
| GET | `/victim/requests/new` | Form tạo yêu cầu mới |
| POST | `/victim/requests` | Gửi yêu cầu mới |
| GET | `/victim/requests/{id}` | Chi tiết yêu cầu (chỉ của mình) |
| GET | `/victim/requests/{id}/edit` | Form sửa yêu cầu (chỉ khi SENT) |
| POST | `/victim/requests/{id}` | Cập nhật yêu cầu (chỉ khi SENT) |
| POST | `/victim/requests/{id}/cancel` | Hủy yêu cầu (chỉ khi SENT) |
| POST | `/victim/requests/{id}/confirm` | Xác nhận đã nhận hỗ trợ (chỉ khi COMPLETED) |

---

## Rescuer (`/rescuer/**` — Role: RESCUER)

### RescuerRequestController — danh sách yêu cầu đang chờ

| Phương thức | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/rescuer/home` | Danh sách yêu cầu SENT; lọc, sắp xếp, phân trang |
| GET | `/rescuer/requests/search` | Alias của `/rescuer/home` (tương đương) |
| GET | `/rescuer/requests/{id}` | Chi tiết yêu cầu (ẩn thông tin Victim) |
| POST | `/rescuer/requests/{id}/accept` | Nhận nhiệm vụ → SENT→ACCEPTED |

### RescuerTaskController — nhiệm vụ của tôi

| Phương thức | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/rescuer/tasks` | Nhiệm vụ đã nhận; lọc, sắp xếp, phân trang |
| GET | `/rescuer/tasks/{id}` | Chi tiết nhiệm vụ (hiển thị thông tin Victim) |
| POST | `/rescuer/tasks/{id}/status` | Cập nhật trạng thái tiếp theo (ACCEPTED→PROCESSING, PROCESSING→COMPLETED) |

---

## Admin (`/admin/**` — Role: ADMIN)

| Phương thức | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/admin/home` | Dashboard: 8 thẻ thống kê + danh sách yêu cầu (lọc, phân trang) |
| GET | `/admin/requests` | Redirect → `/admin/home` (giữ query string nếu có) |
| POST | `/admin/requests/{id}/cancel` | Hủy yêu cầu (trừ CONFIRMED/CANCELLED) |
| GET | `/admin/users` | Danh sách Victim + Rescuer (tab) |
| GET | `/admin/users/{id}` | Chi tiết một Victim: thông tin + lịch sử yêu cầu |
| POST | `/admin/users/{loai}/{id}/toggle` | Khóa/mở khóa tài khoản (`loai` = `victim` hoặc `rescuer`) |
| POST | `/admin/rescuers/{id}/approve` | Duyệt 1 Rescuer |
| POST | `/admin/rescuers/approve-all` | Duyệt tất cả Rescuer đang chờ |

---

## Ghi chú phân quyền

```
SecurityConfig (src/main/java/com/reliefcoordination/relief_coordination/config/SecurityConfig.java):

/                    → permitAll
/login, /register/** → permitAll
/emergency/**        → permitAll
/error, /css/**, /js/** → permitAll
/victim/**           → hasRole("VICTIM")
/rescuer/**          → hasRole("RESCUER")
/admin/**            → hasRole("ADMIN")
/profile, /home      → authenticated (bất kỳ role nào đã đăng nhập)
```

Ngoài phân quyền URL, Service còn kiểm tra **ownership** (ai sở hữu dữ liệu).

