# USER-GUIDE — Hướng dẫn sử dụng Cần cứu trợ

**Điều hướng:** [README](../README.md) · [SPEC](SPEC.md) · [Kiến trúc](ARCHITECTURE.md) · [OOP Design](OOP-DESIGN.md) · [Database](DATABASE.md) · [Workflow](WORKFLOW.md) · [Routes](ROUTES.md) · [Testing](TESTING.md)

---

## 1. Khách — Gửi SOS khẩn cấp (không cần tài khoản)

Dùng khi bạn đang trong tình huống nguy hiểm và **không có thời gian đăng ký tài khoản**.

**Các bước:**

1. Truy cập trang chủ `http://localhost:8080/` → Bấm nút **"Gửi SOS Khẩn cấp"** (hoặc truy cập thẳng `/emergency`).
2. Nhập **Họ tên** và **Số điện thoại** (bắt buộc).
3. Tùy chọn: bấm **"Chia sẻ vị trí GPS"** để hệ thống lấy tọa độ hiện tại của bạn (giúp Rescuer đến đúng chỗ nhanh hơn). Từ chối cũng được.
4. Bấm **"Gửi yêu cầu khẩn cấp"**.
5. Trang xác nhận hiển thị — hệ thống sẽ liên hệ lại qua số điện thoại đã nhập.

> Yêu cầu SOS tự động đặt mức **Khẩn cấp** và loại nhu cầu **Cứu người**. Rescuer sẽ thấy yêu cầu này nổi lên đầu danh sách.

<!-- TODO: chèn ảnh màn hình form SOS -->

---

## 2. Victim — Đăng ký và gửi yêu cầu cứu trợ

### 2.1. Đăng ký tài khoản

1. Trang chủ → Bấm **"Tôi cần cứu trợ"** hoặc truy cập `/register/victim`.
2. Điền: Họ tên, Email, Số điện thoại, Mật khẩu, Nhập lại mật khẩu.
3. Bấm **"Đăng ký"**. Tài khoản được kích hoạt ngay, đăng nhập được luôn.

### 2.2. Đăng nhập

1. Truy cập `/login`.
2. Nhập Email và Mật khẩu đã đăng ký → Bấm **"Đăng nhập"**.
3. Hệ thống chuyển đến trang chủ Victim (`/victim/home`).

### 2.3. Tạo yêu cầu cứu trợ

1. Trang chủ → Bấm **"+ Tạo yêu cầu mới"** hoặc menu **Tạo yêu cầu**.
2. Điền form:
   - **Địa điểm** (bắt buộc): mô tả địa chỉ bằng text.
   - **Lấy vị trí GPS** (tùy chọn): bấm nút, trình duyệt hỏi quyền vị trí — cho phép để tọa độ tự động điền.
   - **Loại nhu cầu** (bắt buộc, chọn nhiều): tích chọn một hoặc nhiều loại (Thực phẩm, Nước uống, Y tế, Nơi ở, Cứu người, Khác).
   - **Số người cần hỗ trợ** (bắt buộc, ≥ 1).
   - **Mức độ khẩn cấp** (bắt buộc): chọn một trong 4 mức.
   - **Mô tả** (tùy chọn): thêm thông tin chi tiết.
3. Bấm **"Gửi yêu cầu"**.

<!-- TODO: chèn ảnh màn hình form tạo yêu cầu -->

### 2.4. Theo dõi yêu cầu

1. Trang chủ hiển thị danh sách tất cả yêu cầu của bạn cùng trạng thái.
2. Bấm **"Xem"** trên một yêu cầu để xem chi tiết:
   - Khi **SENT**: nút Sửa và Hủy.
   - Khi **ACCEPTED/PROCESSING**: thấy tên và SĐT của Rescuer phụ trách.
   - Khi **COMPLETED**: nút **"Xác nhận đã nhận hỗ trợ"** — bấm khi bạn đã nhận được hỗ trợ thực tế.
3. Nếu có GPS, bấm **"Xem bản đồ"** để mở Google Maps với vị trí của bạn.

### 2.5. Sửa / hủy yêu cầu

- Chỉ thực hiện được khi yêu cầu còn ở trạng thái **SENT**.
- Bấm **"Sửa"** để chỉnh sửa thông tin.
- Bấm **"Hủy yêu cầu"** nếu không còn cần hỗ trợ.

<!-- TODO: chèn ảnh màn hình chi tiết yêu cầu -->

---

## 3. Rescuer — Nhận và xử lý yêu cầu

### 3.1. Đăng ký tài khoản

1. Trang chủ → Bấm **"Tôi là người cứu trợ"** hoặc `/register/rescuer`.
2. Điền thông tin, bấm **"Đăng ký"**.
3. Thông báo: "Đăng ký thành công, tài khoản đang chờ Admin xác minh". **Chưa đăng nhập được** cho đến khi Admin duyệt.

### 3.2. Sau khi được duyệt — Xem yêu cầu đang chờ

1. Đăng nhập → trang chủ Rescuer (`/rescuer/home`).
2. Danh sách yêu cầu đang chờ (SENT) được sắp xếp tự động: mới nhất lên đầu.
3. **Lọc theo:** địa điểm (tìm kiếm text), loại nhu cầu, mức khẩn cấp.
4. **Sắp xếp:** mới nhất / cũ nhất.
5. Bấm **"Xem chi tiết"** để xem thông tin yêu cầu (chưa thấy tên và SĐT Victim).

### 3.3. Nhận nhiệm vụ

1. Trang chi tiết yêu cầu → Bấm **"Nhận nhiệm vụ"**.
2. Hệ thống chuyển yêu cầu sang **ACCEPTED** và gán cho bạn.
3. Yêu cầu chuyển sang tab **"Nhiệm vụ của tôi"**.

> Nếu một Rescuer khác vừa nhận trước bạn → thông báo "Yêu cầu đã được người khác nhận".

### 3.4. Cập nhật trạng thái nhiệm vụ

1. Menu **"Nhiệm vụ của tôi"** (`/rescuer/tasks`).
2. Bấm **"Xem"** trên nhiệm vụ cần cập nhật → trang chi tiết.
3. Trang chi tiết nhiệm vụ:
   - Hiển thị tên và SĐT Victim.
   - Link Google Maps (nếu có GPS).
   - Nút cập nhật trạng thái:
     - **ACCEPTED** → Bấm **"Bắt đầu xử lý"** → PROCESSING.
     - **PROCESSING** → Bấm **"Đánh dấu hoàn thành"** → COMPLETED.
4. Sau khi COMPLETED: chờ Victim xác nhận → CONFIRMED.

<!-- TODO: chèn ảnh màn hình nhiệm vụ của tôi -->

---

## 4. Admin — Quản lý hệ thống

### 4.1. Đăng nhập

Sử dụng tài khoản Admin được tạo bởi `DataInitializer` (xem README mục Tài khoản demo).

### 4.2. Dashboard (`/admin/home`)

- Xem **8 thẻ số** thống kê toàn hệ thống.
- Danh sách tất cả yêu cầu; yêu cầu khẩn cấp chưa xử lý nổi lên đầu.
- **Lọc** theo: trạng thái, mức khẩn cấp, loại nhu cầu, từ khóa (tên, địa điểm, SĐT).
- **Hủy yêu cầu**: bấm nút Hủy trên bất kỳ yêu cầu nào chưa CONFIRMED/CANCELLED.

### 4.3. Quản lý tài khoản (`/admin/users`)

- Tab **Victim**: danh sách Victim, nút Khóa/Mở khóa.
- Tab **Rescuer**: danh sách Rescuer với trạng thái (Chờ duyệt / Hoạt động / Bị khóa).
  - Nút **"Duyệt"** cho từng Rescuer chờ duyệt.
  - Nút **"Duyệt tất cả"** để duyệt hàng loạt.
- Bấm vào tên Victim để xem chi tiết: thông tin cá nhân + lịch sử yêu cầu.

<!-- TODO: chèn ảnh màn hình trang quản lý tài khoản -->

---

## 5. Hồ sơ cá nhân (mọi vai trò)

- Truy cập menu **Hồ sơ** hoặc `/profile`.
- Chỉnh sửa: Họ tên, Số điện thoại.
- Đổi mật khẩu: nhập mật khẩu hiện tại, nhập mật khẩu mới và xác nhận.
- Bấm **"Lưu thay đổi"**.

