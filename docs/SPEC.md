# SPEC — Website điều phối cứu trợ thiên tai (Relief Coordination)

> Tài liệu đặc tả duy nhất của dự án. Mọi thành viên và Cursor phải đọc trước khi sửa code.
> Khi có chỗ mâu thuẫn, mục **0. Quyết định đã chốt** được ưu tiên.

## 0. Quyết định đã chốt

**Phạm vi**

- KHÔNG làm: xác thực email/OTP, quên mật khẩu, WebSocket/real-time, upload file/ảnh, biểu đồ, quản lý kho/vật tư/thanh toán, quản lý hồ sơ cá nhân (có thể thêm nếu dư thời gian).
- Bản đồ: chỉ dùng link Google Maps `https://www.google.com/maps?q={lat},{lng}`. Không nhúng Leaflet/OpenStreetMap.
- Thống kê Admin: chỉ dùng thẻ số (card), không vẽ biểu đồ.

**Kỹ thuật**

- Spring Boot 4.1.1, Java 21, Maven, Spring Security 7, Spring Data JPA/Hibernate, Thymeleaf.
- Giao diện: Bootstrap 5 qua CDN, không dùng template admin có sẵn.
- Database: PostgreSQL trên Neon, 1 project dùng chung, mỗi dev một nhánh Neon riêng. Thông tin kết nối nằm trong `.env` (git-ignored).
- `User` là abstract class, kế thừa `JOINED`, 3 lớp con `Victim`/`Rescuer`/`Admin`.
- Trạng thái yêu cầu: `SENT → ACCEPTED → PROCESSING → COMPLETED → CONFIRMED`, thêm `CANCELLED`. Chuyển trạng thái chỉ qua `RequestStatus.canTransitionTo()`.
- Chống nhận trùng yêu cầu: `@Version` (optimistic locking) trên `ReliefRequest`.
- Rescuer tách thành 2 controller: `RescuerRequestController` (danh sách, lọc, chi tiết, nhận) và `RescuerTaskController` (nhiệm vụ của tôi, cập nhật trạng thái).

**Tài khoản**

- Victim và Rescuer tự đăng ký, không cần Admin duyệt.
- Admin chỉ được tạo bởi `DataInitializer` (không có trang đăng ký Admin).
- Admin có thể khóa/mở khóa Victim và Rescuer. Tài khoản bị khóa không đăng nhập được, báo "Tài khoản đã bị khóa".

## 1. Giới thiệu

Nền tảng tập trung kết nối người cần cứu trợ (Victim) với người cứu trợ (Rescuer) khi có thiên tai (bão, lũ, sạt lở), và quản lý toàn bộ vòng đời của một yêu cầu cứu trợ: từ lúc Victim tạo yêu cầu cho đến khi Victim xác nhận đã nhận được hỗ trợ. Admin giám sát tài khoản, yêu cầu và số liệu tổng quan.

Hệ thống chỉ tập trung vào điều phối yêu cầu, phù hợp nhóm 5 người và hạn nộp 12/11.

## 2. Vai trò và chức năng

### 2.1. Victim
- Đăng ký (họ tên, email, SĐT, mật khẩu), đăng nhập, đăng xuất.
- Tạo yêu cầu: địa điểm (text), loại nhu cầu, số người cần hỗ trợ, mức độ khẩn cấp, mô tả.
  - Loại nhu cầu: thực phẩm, nước uống, y tế, chỗ ở, cứu người, khác.
  - Mức độ khẩn cấp: thấp, trung bình, cao, khẩn cấp.
- Lấy vị trí GPS bằng Geolocation API của trình duyệt, **một lần duy nhất** khi bấm nút, lưu `latitude`/`longitude` cùng yêu cầu. Không theo dõi liên tục. GPS không bắt buộc: nếu từ chối quyền vị trí thì vẫn gửi được yêu cầu với địa điểm dạng text.
- Xem danh sách và chi tiết các yêu cầu của mình.
- Sửa và hủy yêu cầu **chỉ khi còn `SENT`**.
- Theo dõi trạng thái; sau khi được nhận, xem tên và SĐT của Rescuer phụ trách.
- Khi yêu cầu `COMPLETED`, bấm xác nhận đã nhận hỗ trợ → `CONFIRMED`.

### 2.2. Rescuer
- Đăng ký, đăng nhập, đăng xuất.
- Dashboard: xem các yêu cầu `SENT`, sắp xếp ưu tiên mức khẩn cấp cao trước, rồi theo thời gian tạo.
- Tìm kiếm theo từ khóa trong địa điểm (khu vực) và lọc theo loại nhu cầu, mức độ khẩn cấp.
- Xem chi tiết yêu cầu trước khi nhận (xem mục 4 về dữ liệu được hiển thị).
- Nhận nhiệm vụ. Một Rescuer được nhận **nhiều** yêu cầu cùng lúc.
- Trang "Nhiệm vụ của tôi": xem các yêu cầu mình đã nhận, cập nhật trạng thái `ACCEPTED → PROCESSING → COMPLETED`.

### 2.3. Admin
- Xem danh sách tài khoản Victim/Rescuer, khóa hoặc mở khóa (không khóa được tài khoản Admin, kể cả chính mình).
- Xem toàn bộ yêu cầu, hủy yêu cầu giả/spam/không hợp lệ.
- Xem thống kê tổng quan (mục 5).
- Admin không trực tiếp xử lý yêu cầu cứu trợ.

## 3. Vòng đời yêu cầu

Luồng chính: `SENT → ACCEPTED → PROCESSING → COMPLETED → CONFIRMED`.

**Các bước chuyển hợp lệ và người được thực hiện**

- `SENT` → `ACCEPTED`: Rescuer bất kỳ (nhận yêu cầu, gán `rescuer`)
- `SENT` → `CANCELLED`: Victim chủ yêu cầu, hoặc Admin
- `ACCEPTED` → `PROCESSING`: Rescuer đã nhận yêu cầu đó
- `PROCESSING` → `COMPLETED`: Rescuer đã nhận yêu cầu đó
- `COMPLETED` → `CONFIRMED`: Victim chủ yêu cầu
- `ACCEPTED`, `PROCESSING`, `COMPLETED` → `CANCELLED`: Chỉ Admin

**Quy tắc**

- `CONFIRMED` và `CANCELLED` là trạng thái cuối, không chuyển tiếp nữa.
- Mọi chuyển trạng thái đều phải qua `RequestStatus.canTransitionTo()` **và** kiểm tra người thực hiện trong Service.
- Nếu hai Rescuer cùng nhận một yêu cầu, `@Version` làm một bên thất bại: bắt `ObjectOptimisticLockingFailureException` và hiện thông báo "Yêu cầu đã được người khác nhận", không hiện trang lỗi.

## 4. Quyền riêng tư và phân quyền dữ liệu

- Victim chỉ thấy, sửa, hủy, xác nhận yêu cầu **của mình**.
- Rescuer chỉ cập nhật trạng thái yêu cầu **mình đã nhận**.
- Trước khi nhận, Rescuer thấy: mã, địa điểm, link bản đồ, loại nhu cầu, số người, mức độ khẩn cấp, mô tả, thời gian tạo. **Không** thấy họ tên và SĐT của Victim.
- Sau khi nhận, Rescuer thấy thêm họ tên và SĐT của Victim.
- Victim chỉ thấy họ tên và SĐT của Rescuer sau khi yêu cầu được nhận.
- Mật khẩu luôn mã hóa BCrypt qua `PasswordEncoder`.
- Kiểm tra quyền ở tầng Service, không chỉ dựa vào URL.

## 5. Thống kê Admin (thẻ số)

- **Tổng số yêu cầu**: Tất cả yêu cầu
- **Đang chờ**: Số yêu cầu `SENT`
- **Đang xử lý**: Số yêu cầu `ACCEPTED` + `PROCESSING`
- **Hoàn thành**: Số yêu cầu `COMPLETED`
- **Đã xác nhận**: Số yêu cầu `CONFIRMED`
- **Đã hủy**: Số yêu cầu `CANCELLED`
- **Số người đã được hỗ trợ**: Tổng `numberOfPeople` của các yêu cầu `CONFIRMED`

## 6. Cấu trúc URL

**Công khai**

- `GET /` → Landing cho người chưa đăng nhập: chọn "Tôi cần cứu trợ" / "Tôi là người cứu trợ". Người đã đăng nhập tự chuyển sang `/home`
- `GET, POST /login` → Đăng nhập
- `POST /logout` → Đăng xuất
- `GET, POST /register/victim` → Đăng ký Victim
- `GET, POST /register/rescuer` → Đăng ký Rescuer

**Điểm vào chung** (cần đăng nhập)

- `GET /home` → `HomeController` chuyển về trang chủ đúng role: `/victim/home`, `/rescuer/home` hoặc `/admin/home`

**Victim** (`/victim/**`, role VICTIM)

- `GET /victim/home` → Dashboard, danh sách yêu cầu của tôi
- `GET, POST /victim/requests/new` → Tạo yêu cầu
- `GET /victim/requests/{id}` → Chi tiết
- `GET, POST /victim/requests/{id}/edit` → Sửa (chỉ SENT)
- `POST /victim/requests/{id}/cancel` → Hủy (chỉ SENT)
- `POST /victim/requests/{id}/confirm` → Xác nhận đã nhận hỗ trợ

**Rescuer** (`/rescuer/**`, role RESCUER)

- `GET /rescuer/home` → Dashboard: yêu cầu SENT, tìm kiếm, lọc (RescuerRequestController)
- `GET /rescuer/requests/{id}` → Chi tiết trước khi nhận (RescuerRequestController)
- `POST /rescuer/requests/{id}/accept` → Nhận nhiệm vụ (RescuerRequestController)
- `GET /rescuer/tasks` → Nhiệm vụ của tôi (RescuerTaskController)
- `GET /rescuer/tasks/{id}` → Chi tiết nhiệm vụ (RescuerTaskController)
- `POST /rescuer/tasks/{id}/status` → Cập nhật trạng thái (RescuerTaskController)

**Admin** (`/admin/**`, role ADMIN)

- `GET /admin/home` → Dashboard thống kê
- `GET /admin/users` → Danh sách tài khoản
- `POST /admin/users/{id}/toggle-lock` → Khóa / mở khóa
- `GET /admin/requests` → Danh sách mọi yêu cầu
- `POST /admin/requests/{id}/cancel` → Hủy yêu cầu

## 7. Giao diện

- Trang con dùng `fragments/layout.html`; mỗi trang bọc nội dung trong `<div th:fragment="content">`.
- Landing, login, register là **trang công khai độc lập**, không dùng `layout.html` (layout có tên người dùng và nút Đăng xuất, chỉ dành cho người đã đăng nhập).
- `layout.html` đã chốt, không sửa. Menu theo role nằm ở 3 file riêng trong `templates/fragments/`: `nav-victim.html`, `nav-rescuer.html`, `nav-admin.html`. Người phụ trách role nào chỉ sửa file menu của role đó (đổi chữ, thêm link).
- Menu mặc định: Victim (Yêu cầu của tôi, Tạo yêu cầu); Rescuer (Yêu cầu chờ, Nhiệm vụ của tôi); Admin (Thống kê, Tài khoản, Yêu cầu). Mục đầu tiên của mỗi menu là trang chủ của role.
- Chữ hiển thị trên giao diện bằng tiếng Việt; tên class, biến, method bằng tiếng Anh.
- Form dùng form object/DTO có `@Valid`, hiển thị lỗi validate cạnh từng trường.
- Màu badge trạng thái thống nhất: SENT (xám), ACCEPTED (xanh dương), PROCESSING (vàng), COMPLETED (xanh lá nhạt), CONFIRMED (xanh lá), CANCELLED (đỏ).

> Các sơ đồ 7.1 đến 7.11 là phác thảo giao diện đích để tham khảo. Hiện chỉ có Landing, đăng nhập và 3 trang `home`; các màn còn lại do từng người làm ở Bước 1. Bắt buộc đúng các trường, nút, quy tắc hiển thị và URL; bố cục và màu sắc tự do.

### 7.1. Landing (`/`)

```
┌────────────────────────────────────────────────────┐
│                RELIEF COORDINATION                 │
│                 Bạn cần hỗ trợ gì?                 │
│                                                    │
│     [ Tôi cần cứu trợ ]   →  /register/victim      │
│                                                    │
│  [ Tôi là người cứu trợ ]   →  /register/rescuer   │
│                                                    │
│       Đã có tài khoản? Đăng nhập  →  /login        │
│                                                    │
└────────────────────────────────────────────────────┘
```

### 7.2. Đăng ký (`/register/victim`, `/register/rescuer`) và Đăng nhập (`/login`)

```
┌────────────────────────────────────────────┐
│        ĐĂNG KÝ  —  Victim / Rescuer        │
├────────────────────────────────────────────┤
│ Họ tên      [______________________]       │
│ Email       [______________________]       │
│ SĐT         [______________________]       │
│ Mật khẩu    [______________________]       │
│ Nhập lại    [______________________]       │
│                                            │
│              [   Đăng ký   ]               │
│                                            │
│         Đã có tài khoản? Đăng nhập         │
└────────────────────────────────────────────┘

┌────────────────────────────────────────────┐
│                 ĐĂNG NHẬP                  │
├────────────────────────────────────────────┤
│ Email       [______________________]       │
│ Mật khẩu    [______________________]       │
│                                            │
│             [   Đăng nhập   ]              │
│                                            │
│         Chưa có tài khoản? Đăng ký         │
└────────────────────────────────────────────┘
```

### 7.3. Victim — Dashboard (`/victim/home`)

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│ RELIEF  |  Yêu cầu của tôi  |  Tạo yêu cầu        Xin chào, [Tên]  [Đăng xuất]     │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│ [ + Tạo yêu cầu mới ]                                                              │
│                                                                                    │
│ Yêu cầu của tôi                                                                    │
│   #12   Thực phẩm   Khẩn cấp     SENT        [Xem]                                 │
│   #08   Y tế        Cao          ACCEPTED    [Xem]                                 │
│   #03   Nơi ở       Trung bình   COMPLETED   [Xác nhận]                            │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.4. Victim — Tạo / sửa yêu cầu (`/victim/requests/new`, `/victim/requests/{id}/edit`)

```
┌──────────────────────────────────────────────────────────────┐
│                     TẠO YÊU CẦU CỨU TRỢ                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│ Địa điểm        [____________________________]               │
│                                                              │
│ [ Lấy vị trí GPS hiện tại ]   (thành công: "Đã lấy vị trí")  │
│                                                              │
│ Loại nhu cầu    [▼ Thực phẩm                  ]              │
│ Số người cần    [____]                                       │
│ Mức độ khẩn     [▼ Trung bình                 ]              │
│ Mô tả           [____________________________]               │
│                 [____________________________]               │
│                                                              │
│                [ Hủy ]        [ Gửi yêu cầu ]                │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 7.5. Victim — Chi tiết yêu cầu (`/victim/requests/{id}`)

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│ RELIEF  |  Yêu cầu của tôi  |  Tạo yêu cầu        Xin chào, [Tên]  [Đăng xuất]     │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│ Yêu cầu #12                                  Trạng thái: [SENT]                    │
│                                                                                    │
│ Địa điểm       : Khu vực 1, Quảng Nam                                              │
│ Loại nhu cầu   : Thực phẩm                                                         │
│ Số người       : 3                                                                 │
│ Mức độ khẩn    : Cao                                                               │
│ Mô tả          : ...                                                               │
│ Bản đồ         : [Xem bản đồ]   (chỉ hiện khi có GPS)                              │
│ Người cứu trợ  : (chưa có)                                                         │
│                  → sau khi được nhận: Tên + SĐT                                    │
│                                                                                    │
│ Khi SENT        :  [ Sửa ]   [ Hủy yêu cầu ]                                       │
│ Khi COMPLETED   :  [ Xác nhận đã nhận hỗ trợ ]                                     │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.6. Rescuer — Dashboard (`/rescuer/home`)

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│ RELIEF  |  Yêu cầu chờ  |  Nhiệm vụ của tôi        Xin chào, [Tên]  [Đăng xuất]    │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│ Tìm khu vực: [____________]   Loại: [▼ Tất cả]   Khẩn cấp: [▼ Tất cả]   [ Lọc ]    │
│                                                                                    │
│ Yêu cầu đang chờ (chỉ SENT, khẩn cấp lên trước)                                    │
│   #15   Y tế         Khẩn cấp   10:20   [Xem chi tiết]                             │
│   #12   Thực phẩm    Cao        09:45   [Xem chi tiết]                             │
│   #20   Nước uống    Cao        09:10   [Xem chi tiết]                             │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.7. Rescuer — Chi tiết yêu cầu trước khi nhận (`/rescuer/requests/{id}`)

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│ RELIEF  |  Yêu cầu chờ  |  Nhiệm vụ của tôi        Xin chào, [Tên]  [Đăng xuất]    │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│ Yêu cầu #15                                  Trạng thái: [SENT]                    │
│                                                                                    │
│ Địa điểm       : Khu vực 5, Quảng Nam                                              │
│ Loại nhu cầu   : Y tế                                                              │
│ Số người       : 2                                                                 │
│ Mức độ khẩn    : Khẩn cấp                                                          │
│ Mô tả          : ...                                                               │
│ Bản đồ         : [Xem bản đồ]                                                      │
│ Người cần giúp : (ẩn tên và SĐT cho đến khi nhận)                                  │
│                                                                                    │
│ [ Nhận nhiệm vụ ]                                                                  │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.8. Rescuer — Nhiệm vụ của tôi (`/rescuer/tasks`, `/rescuer/tasks/{id}`)

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│ RELIEF  |  Yêu cầu chờ  |  Nhiệm vụ của tôi        Xin chào, [Tên]  [Đăng xuất]    │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│ Nhiệm vụ của tôi                                                                   │
│   #08   Y tế       Cao   ACCEPTED     [Xem / Cập nhật]                             │
│   #05   Nước uống  TB    PROCESSING   [Xem / Cập nhật]                             │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────────────┐
│ RELIEF  |  Yêu cầu chờ  |  Nhiệm vụ của tôi        Xin chào, [Tên]  [Đăng xuất]    │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│ Yêu cầu #08                                 Trạng thái: [ACCEPTED]                 │
│                                                                                    │
│ Người cần giúp : Lê Văn A   —   SĐT 0900000011                                     │
│ Địa điểm       : Khu vực 3, Quảng Nam   [Xem bản đồ]                               │
│                                                                                    │
│ Chỉ hiện nút cho bước hợp lệ tiếp theo:                                            │
│   ACCEPTED    →  [ Bắt đầu xử lý ]                                                 │
│   PROCESSING  →  [ Đánh dấu hoàn thành ]                                           │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.9. Admin — Dashboard (`/admin/home`)

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│ RELIEF ADMIN  |  Thống kê  |  Tài khoản  |  Yêu cầu        [Đăng xuất]             │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│ Thống kê tổng quan                                                                 │
│                                                                                    │
│   [ Tổng yêu cầu: 20 ]   [ Đang chờ: 7 ]    [ Đang xử lý: 6 ]                      │
│   [ Hoàn thành: 2 ]      [ Đã xác nhận: 3 ]  [ Đã hủy: 2 ]                         │
│   [ Người được hỗ trợ: 8 ]                                                         │
│                                                                                    │
│   [ Quản lý tài khoản ]      [ Quản lý yêu cầu ]                                   │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.10. Admin — Tài khoản (`/admin/users`)

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│ RELIEF ADMIN  |  Thống kê  |  Tài khoản  |  Yêu cầu        [Đăng xuất]             │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│ Danh sách tài khoản                                                                │
│   Họ tên            Email                  Vai trò    Trạng thái   Thao tác        │
│   ----------------  ---------------------  ---------  -----------  --------------  │
│   Nguyen Van Cuu    rescuer1@relief.vn     Rescuer    Hoạt động    [Khóa]          │
│   Le Van A          victim1@relief.vn      Victim     Hoạt động    [Khóa]          │
│   Pham Thi B        victim2@relief.vn      Victim     Đã khóa      [Mở khóa]       │
│                                                                                    │
│ (không có nút khóa cho tài khoản Admin)                                            │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.11. Admin — Yêu cầu (`/admin/requests`)

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│ RELIEF ADMIN  |  Thống kê  |  Tài khoản  |  Yêu cầu        [Đăng xuất]             │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│ Tất cả yêu cầu                                                                     │
│   Mã    Người gửi     Loại        Khẩn cấp   Trạng thái   Thao tác                 │
│   ----  ------------  ----------  ---------  -----------  ----------------         │
│   #12   Le Van A      Thực phẩm   Cao        SENT         [Xem]  [Hủy]             │
│   #08   Pham Thi B    Y tế        Khẩn cấp   PROCESSING   [Xem]  [Hủy]             │
│   #03   Hoang Van C   Nơi ở       TB         CONFIRMED    [Xem]                    │
│                                                                                    │
│ (nút Hủy ẩn với yêu cầu đã CONFIRMED hoặc CANCELLED)                               │
│                                                                                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

## 8. Quy ước kỹ thuật chung

**Entity `ReliefRequest`** (không sửa khi chưa hỏi nhóm trưởng)

- Trạng thái chỉ đổi qua `request.changeStatus(next)`; `setStatus` đã bị khóa (`@Setter(AccessLevel.NONE)`). Chỉ `DataInitializer` được đi qua các trạng thái để seed dữ liệu.
- Rescuer nhận yêu cầu bằng `request.accept(rescuer)` (kiểm tra bước đi trước, rồi mới gán rescuer). Không gọi `setRescuer(...)` trực tiếp trong Service.
- `isEditable()` cho biết Victim còn sửa/hủy được không (chỉ khi `SENT`). `getMapUrl()` trả link Google Maps hoặc `null` nếu không có GPS; `hasLocation()` kiểm tra có tọa độ.
- Tên các field chính: `victim`, `rescuer`, `location`, `latitude`, `longitude`, `needType`, `numberOfPeople`, `urgencyLevel`, `description`, `status`, `createdAt`, `updatedAt`, `version`.
- `changeStatus()` ném `IllegalStateException` khi sai bước: Controller phải bắt và hiện thông báo thân thiện.

**Phân chia file**

- `pom.xml`, `SecurityConfig`, `application.properties` → **Chỉ nhóm trưởng**
- `model/`, `layout.html`, `DataInitializer` → **Chỉ nhóm trưởng (xin qua nhóm trưởng)**
- `nav-<role>.html`, controller / service / form / template của role mình → **Người phụ trách role đó**
- `ReliefRequestRepository` → **Mọi người, chỉ thêm method mới ở cuối file**

**Dữ liệu mẫu và tài khoản thử** (do `DataInitializer` tạo khi bảng `users` trống)

- **Admin**: `admin@relief.vn` — mật khẩu `admin123`
- **Rescuer**: `rescuer1@relief.vn`, `rescuer2@relief.vn` — mật khẩu `rescuer123`
- **Victim**: `victim1@relief.vn`, `victim2@relief.vn`, `victim3@relief.vn` — mật khẩu `victim123`

Có sẵn 20 yêu cầu mẫu: SENT 7, ACCEPTED 3, PROCESSING 3, COMPLETED 2, CONFIRMED 3, CANCELLED 2. Yêu cầu có số thứ tự lẻ (1, 3, 5...) có tọa độ GPS để thử link bản đồ.

**Cơ sở dữ liệu**

- Mỗi người dùng một nhánh Neon riêng và file `.env` riêng; `.env` không bao giờ commit, chỉ commit `.env.example`.
- Muốn seed lại: chạy `TRUNCATE TABLE users RESTART IDENTITY CASCADE;` trên nhánh Neon của chính mình rồi chạy lại app.

## 9. Công nghệ

- Backend: Java 21, Spring Boot 4.1.1, Spring Security 7, Spring Data JPA, Hibernate, Maven, Lombok, DevTools.
- Frontend: Thymeleaf, Bootstrap 5 (CDN), JavaScript thuần (chỉ cho nút GPS).
- Database: PostgreSQL (Neon). Dev local có thể dùng Docker làm phương án dự phòng.
- Quản lý mã nguồn: Git/GitHub, mỗi người một nhánh `feature/<tên>-<chức năng>`, merge vào `main` qua Pull Request.

## 10. Quy trình tổng thể

1. Victim đăng nhập, tạo yêu cầu, (tùy chọn) lấy GPS → trạng thái `SENT`.
2. Rescuer đăng nhập, xem danh sách, tìm/lọc, xem chi tiết.
3. Rescuer nhận một hoặc nhiều yêu cầu → `ACCEPTED`.
4. Rescuer hỗ trợ và cập nhật → `PROCESSING` → `COMPLETED`.
5. Victim kiểm tra và xác nhận → `CONFIRMED`.
6. Admin giám sát tài khoản, yêu cầu, thống kê; hủy yêu cầu không hợp lệ.

## 11. Hướng mở rộng (chỉ làm nếu dư thời gian)

Hồ sơ cá nhân, thông báo trong trang, đánh giá Rescuer.
