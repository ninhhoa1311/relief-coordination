# ĐỀ TÀI: WEBSITE ĐIỀU PHỐI CỨU TRỢ THIÊN TAI

## 1. Giới thiệu đề tài

### 1.1. Bối cảnh

Trong những năm gần đây, các hiện tượng thiên tai như bão, lũ lụt, sạt lở đất có thể gây ảnh hưởng nghiêm trọng đến đời sống của người dân. Trong những tình huống này, nhu cầu cứu trợ thường xuất hiện ở nhiều khu vực cùng một thời điểm. Người dân cần hỗ trợ có thể gặp khó khăn trong việc tìm kiếm người cứu trợ, trong khi những cá nhân hoặc đội nhóm muốn tham gia cứu trợ lại khó xác định chính xác khu vực nào đang cần hỗ trợ, cần hỗ trợ về vấn đề gì và mức độ khẩn cấp ra sao.

Hiện nay, việc kết nối giữa người cần cứu trợ và người cứu trợ thường được thực hiện thông qua điện thoại, tin nhắn hoặc các nền tảng mạng xã hội. Tuy nhiên, những phương thức này thường mang tính tự phát, thông tin bị phân tán và khó theo dõi toàn bộ quá trình xử lý của một yêu cầu cứu trợ.

Từ thực tế trên, nhóm đề xuất xây dựng "Website điều phối cứu trợ thiên tai" nhằm tạo ra một nền tảng tập trung để kết nối người cần cứu trợ với người cứu trợ, đồng thời quản lý quá trình xử lý yêu cầu một cách rõ ràng và có hệ thống.

### 1.2. Ý tưởng của đề tài

Website được xây dựng với mục tiêu kết nối trực tiếp hai nhóm chính là người cần cứu trợ và người cứu trợ. Người dân có thể gửi yêu cầu hỗ trợ thông qua website, trong đó cung cấp thông tin về nhu cầu, số lượng người cần hỗ trợ, mức độ khẩn cấp và vị trí hiện tại.

Người cứu trợ có thể xem danh sách các yêu cầu đang chờ xử lý, tìm kiếm và lọc các yêu cầu theo khu vực, loại nhu cầu hoặc mức độ khẩn cấp. Sau khi lựa chọn một yêu cầu phù hợp, người cứu trợ có thể nhận nhiệm vụ, tiến hành hỗ trợ và cập nhật trạng thái của yêu cầu.

Bên cạnh đó, quản trị viên có nhiệm vụ giám sát hoạt động của hệ thống, quản lý tài khoản, kiểm soát các yêu cầu không hợp lệ và theo dõi các số liệu thống kê tổng quan.

Hệ thống tập trung vào việc điều phối yêu cầu cứu trợ, không triển khai các nghiệp vụ phức tạp như quản lý kho hàng, quản lý vật tư hay thanh toán. Điều này giúp phạm vi của đề tài phù hợp với thời gian thực hiện và số lượng thành viên trong nhóm.

## 2. Đối tượng sử dụng hệ thống

Hệ thống gồm ba nhóm người dùng:

- Người cần cứu trợ (Victim).
- Người cứu trợ (Rescuer).
- Quản trị viên (Admin).

Mỗi nhóm có quyền truy cập và chức năng khác nhau.

### 2.1. Người cần cứu trợ (Victim)

Victim là người dân đang gặp khó khăn do thiên tai và cần sự hỗ trợ từ người khác.

Các chức năng chính:

**a. Đăng ký và đăng nhập**

Người dùng có thể tạo tài khoản và đăng nhập vào hệ thống bằng các thông tin cơ bản như họ tên, email, mật khẩu và số điện thoại.

**b. Tạo yêu cầu cứu trợ**

Victim có thể tạo một yêu cầu cứu trợ và cung cấp các thông tin:

- Địa điểm cần hỗ trợ.
- Loại nhu cầu.
- Số người cần hỗ trợ.
- Mức độ khẩn cấp.
- Mô tả tình hình.

Các loại nhu cầu có thể bao gồm thực phẩm, nước uống, y tế, chỗ ở, cứu người và các nhu cầu khác.

Mức độ khẩn cấp được chia thành các mức: thấp, trung bình, cao và khẩn cấp.

**c. Lấy vị trí hiện tại**

Khi tạo yêu cầu, Victim có thể sử dụng chức năng lấy vị trí hiện tại của thiết bị.

Hệ thống sử dụng Geolocation API của trình duyệt để lấy tọa độ gồm kinh độ và vĩ độ. Vị trí này được lưu cùng với yêu cầu cứu trợ.

Chức năng chỉ lấy vị trí tại thời điểm người dùng yêu cầu, không theo dõi vị trí liên tục.

**d. Quản lý yêu cầu cứu trợ**

Victim có thể xem danh sách các yêu cầu đã tạo và xem thông tin chi tiết của từng yêu cầu.

Trong trường hợp yêu cầu chưa được Rescuer tiếp nhận, Victim có thể chỉnh sửa hoặc hủy yêu cầu.

**e. Theo dõi trạng thái**

Victim có thể theo dõi quá trình xử lý yêu cầu thông qua các trạng thái của hệ thống.

**f. Xem thông tin người cứu trợ**

Sau khi yêu cầu được Rescuer tiếp nhận, Victim có thể xem một số thông tin cơ bản của người cứu trợ đang xử lý yêu cầu.

**g. Xác nhận đã nhận hỗ trợ**

Sau khi Rescuer đánh dấu yêu cầu hoàn thành, Victim có thể xác nhận rằng mình đã thực sự nhận được hỗ trợ.

## 3. Người cứu trợ (Rescuer)

Rescuer là cá nhân hoặc đội nhóm tham gia hoạt động cứu trợ.

Các chức năng chính:

**a. Đăng nhập**

Rescuer đăng nhập vào hệ thống bằng tài khoản đã được cung cấp.

**b. Xem danh sách yêu cầu cứu trợ**

Rescuer có thể xem các yêu cầu đang chờ được hỗ trợ.

Danh sách có thể hiển thị các thông tin như:

- Mã yêu cầu.
- Khu vực.
- Loại nhu cầu.
- Số người cần hỗ trợ.
- Mức độ khẩn cấp.
- Thời gian tạo.
- Trạng thái.

**c. Tìm kiếm và lọc yêu cầu**

Rescuer có thể tìm kiếm và lọc yêu cầu theo:

- Khu vực.
- Loại nhu cầu.
- Mức độ khẩn cấp.

Hệ thống có thể ưu tiên hiển thị các yêu cầu có mức độ khẩn cấp cao hơn.

**d. Xem chi tiết yêu cầu**

Rescuer có thể xem đầy đủ thông tin của một yêu cầu trước khi nhận nhiệm vụ, bao gồm thông tin nhu cầu, số người cần hỗ trợ, mô tả tình hình và vị trí.

**e. Xem vị trí người cần cứu trợ**

Rescuer có thể xem tọa độ GPS do Victim cung cấp. Nếu triển khai chức năng bản đồ, vị trí có thể được hiển thị trực quan trên bản đồ.

**f. Nhận nhiệm vụ**

Rescuer có thể lựa chọn một yêu cầu phù hợp và nhận làm nhiệm vụ.

Sau khi nhận, yêu cầu được gán cho Rescuer tương ứng. Một Rescuer có thể nhận nhiều yêu cầu nếu có khả năng xử lý nhiều trường hợp.

**g. Quản lý nhiệm vụ**

Rescuer có một khu vực riêng để xem các yêu cầu mà mình đã nhận và theo dõi quá trình xử lý.

**h. Cập nhật trạng thái**

Trong quá trình cứu trợ, Rescuer có thể cập nhật trạng thái của yêu cầu:

Đã tiếp nhận → Đang xử lý → Hoàn thành.

## 4. Quản trị viên (Admin)

Admin là người quản lý và giám sát hoạt động của hệ thống.

Admin không trực tiếp tham gia xử lý các yêu cầu cứu trợ.

Các chức năng chính:

**a. Quản lý tài khoản**

Admin có thể xem danh sách tài khoản của Victim và Rescuer.

Khi phát hiện tài khoản có hành vi spam, gian lận hoặc vi phạm quy định, Admin có thể khóa tài khoản.

**b. Quản lý yêu cầu cứu trợ**

Admin có thể xem toàn bộ các yêu cầu được tạo trên hệ thống.

Nếu phát hiện yêu cầu giả, spam hoặc không hợp lệ, Admin có thể hủy yêu cầu.

**c. Thống kê**

Admin có thể xem các số liệu tổng quan của hệ thống, chẳng hạn:

- Tổng số yêu cầu.
- Số yêu cầu đang chờ xử lý.
- Số yêu cầu đang được xử lý.
- Số yêu cầu đã hoàn thành.
- Số yêu cầu đã được xác nhận.
- Số người đã nhận được hỗ trợ.

## 5. Vòng đời của một yêu cầu cứu trợ

Mỗi yêu cầu cứu trợ được quản lý thông qua một chu trình gồm năm trạng thái:

Đã gửi → Đã tiếp nhận → Đang xử lý → Hoàn thành → Đã xác nhận.

### 5.1. Đã gửi

Victim vừa tạo yêu cầu và yêu cầu đang chờ Rescuer tiếp nhận.

### 5.2. Đã tiếp nhận

Một Rescuer đã nhận yêu cầu và chịu trách nhiệm xử lý.

### 5.3. Đang xử lý

Rescuer đang tiến hành hỗ trợ Victim.

### 5.4. Hoàn thành

Rescuer xác nhận rằng quá trình hỗ trợ đã hoàn tất.

### 5.5. Đã xác nhận

Victim xác nhận rằng mình đã thực sự nhận được hỗ trợ.

Hệ thống chỉ cho phép yêu cầu chuyển trạng thái theo đúng trình tự, hạn chế việc trạng thái bị thay đổi không hợp lệ.

## 6. Quy trình hoạt động tổng thể

Quy trình hoạt động chính của hệ thống được thực hiện như sau:

- Bước 1: Victim đăng nhập vào hệ thống.
- Bước 2: Victim tạo yêu cầu cứu trợ và nhập các thông tin cần thiết.
- Bước 3: Victim lấy vị trí hiện tại thông qua chức năng GPS.
- Bước 4: Hệ thống lưu yêu cầu vào cơ sở dữ liệu với trạng thái "Đã gửi".
- Bước 5: Rescuer đăng nhập và xem danh sách các yêu cầu đang chờ xử lý.
- Bước 6: Rescuer tìm kiếm, lọc và xem chi tiết các yêu cầu.
- Bước 7: Rescuer lựa chọn và nhận một hoặc nhiều nhiệm vụ.
- Bước 8: Hệ thống chuyển trạng thái yêu cầu sang "Đã tiếp nhận".
- Bước 9: Rescuer tiến hành hỗ trợ và cập nhật trạng thái sang "Đang xử lý".
- Bước 10: Sau khi hỗ trợ xong, Rescuer chuyển trạng thái sang "Hoàn thành".
- Bước 11: Victim kiểm tra và xác nhận đã nhận được hỗ trợ.
- Bước 12: Hệ thống chuyển yêu cầu sang trạng thái "Đã xác nhận".

Trong toàn bộ quá trình, Admin có thể giám sát tài khoản, yêu cầu và các số liệu thống kê của hệ thống.

## 7. Các chức năng chính của hệ thống

Có thể tổng hợp các chức năng của website như sau:

**Chức năng chung**

- Đăng ký.
- Đăng nhập.
- Đăng xuất.
- Phân quyền người dùng.
- Quản lý thông tin cá nhân.

**Chức năng Victim**

- Tạo yêu cầu cứu trợ.
- Lấy vị trí hiện tại.
- Xem danh sách yêu cầu.
- Xem chi tiết yêu cầu.
- Chỉnh sửa yêu cầu.
- Hủy yêu cầu.
- Theo dõi trạng thái.
- Xem thông tin Rescuer.
- Xác nhận đã nhận hỗ trợ.

**Chức năng Rescuer**

- Đăng nhập.
- Xem danh sách yêu cầu.
- Tìm kiếm và lọc yêu cầu.
- Xem chi tiết yêu cầu.
- Xem vị trí Victim.
- Nhận nhiệm vụ.
- Quản lý nhiệm vụ.
- Cập nhật trạng thái.
- Hoàn thành nhiệm vụ.

**Chức năng Admin**

- Quản lý tài khoản.
- Khóa tài khoản.
- Xem toàn bộ yêu cầu.
- Hủy yêu cầu không hợp lệ.
- Xem thống kê tổng quan.

## 8. Công nghệ dự kiến sử dụng

### 8.1. Backend

- Java 21.
- Spring Boot.
- Spring Security.
- Spring Data JPA.
- Hibernate.
- Maven.

### 8.2. Frontend

- HTML.
- CSS.
- JavaScript.
- Thymeleaf.

### 8.3. Cơ sở dữ liệu

- PostgreSQL.

### 8.4. Chức năng vị trí

- Browser Geolocation API để lấy vị trí hiện tại.
- Có thể sử dụng Leaflet và OpenStreetMap để hiển thị vị trí trên bản đồ.

### 8.5. Quản lý mã nguồn

- Git.
- GitHub.

## 9. Phạm vi của đề tài

Để đảm bảo phù hợp với thời gian thực hiện và nhóm gồm năm thành viên, đề tài tập trung vào các nghiệp vụ cốt lõi của việc điều phối cứu trợ.

Các chức năng chính bao gồm:

Đăng ký/Đăng nhập → Phân quyền → Tạo yêu cầu → Lấy vị trí → Tìm kiếm yêu cầu → Nhận nhiệm vụ → Cập nhật trạng thái → Hoàn thành → Xác nhận → Thống kê.

Đề tài không tập trung vào các chức năng phức tạp như quản lý kho hàng, quản lý vật tư, thanh toán, AI, chat realtime hoặc GPS realtime.

Nếu hoàn thành các chức năng chính trước thời hạn, nhóm có thể phát triển thêm chức năng hiển thị vị trí trên bản đồ, upload hình ảnh hoặc thông báo.

## 10. Kết luận

Website điều phối cứu trợ thiên tai là một hệ thống nhằm hỗ trợ kết nối giữa người cần cứu trợ và người cứu trợ trong các tình huống thiên tai.

Điểm cốt lõi của hệ thống là quản lý toàn bộ vòng đời của một yêu cầu cứu trợ, từ khi Victim tạo yêu cầu, cung cấp vị trí, Rescuer tiếp nhận và xử lý, cho đến khi Victim xác nhận đã nhận được hỗ trợ.

Thông qua đề tài, nhóm có thể áp dụng các kiến thức về lập trình hướng đối tượng, cơ sở dữ liệu và phát triển ứng dụng web, đồng thời thực hành các kỹ thuật như kế thừa, đóng gói, trừu tượng, đa hình, phân quyền người dùng và xử lý nghiệp vụ trong một hệ thống thực tế.

## Các quyết định đã chốt (khác với bản đề cương gốc)

- Không làm: xác thực email/OTP, quên mật khẩu, real-time (WebSocket), upload file/ảnh, biểu đồ.
- Bản đồ: chỉ dùng link trỏ sang Google Maps (`https://www.google.com/maps?q={lat},{lng}`), không nhúng Leaflet/OpenStreetMap.
- Giao diện: Bootstrap 5 qua CDN, không dùng template admin có sẵn.
- Database: PostgreSQL trên Neon (cloud), dùng chung 1 project cho cả nhóm.
- Kiến trúc dữ liệu người dùng: `User` là abstract class, kế thừa `JOINED` strategy, 3 class con `Victim`/`Rescuer`/`Admin`.
- Trạng thái yêu cầu: SENT → ACCEPTED → PROCESSING → COMPLETED → CONFIRMED, có thêm CANCELLED. Chuyển trạng thái chỉ qua `RequestStatus.canTransitionTo()`.
- Chống nhận trùng yêu cầu: dùng `@Version` (optimistic locking) trên `ReliefRequest`.
- Thống kê Admin: chỉ cần thẻ số (card), không vẽ biểu đồ.
- `RescuerController` sẽ tách thành `RescuerRequestController` (danh sách, lọc, chi tiết, nhận) và `RescuerTaskController` (nhiệm vụ của tôi, cập nhật trạng thái).


## Sơ đồ màn hình — Relief Coordination

### 1. Trang Landing
```
┌─────────────────────────────────────┐
│        RELIEF COORDINATION            │
│                                       │
│         Bạn cần hỗ trợ gì?           │
│                                       │
│   [ 🆘 Tôi cần cứu trợ ]              │
│   [ 🚑 Tôi là người cứu trợ ]         │
│                                       │
│        Đã có tài khoản? Đăng nhập    │
└─────────────────────────────────────┘
```

### 2. Trang Đăng ký
```
┌─────────────────────────────────────┐
│  Đăng ký — [Victim/Rescuer]          │
├─────────────────────────────────────┤
│  Họ tên     [________________]       │
│  Email      [________________]       │
│  SĐT        [________________]       │
│  Mật khẩu   [________________]       │
│  Nhập lại   [________________]       │
│                                       │
│         [   Đăng ký   ]              │
│    Đã có tài khoản? Đăng nhập        │
└─────────────────────────────────────┘
```

### 3. Trang Login
```
┌─────────────────────────────────┐
│        RELIEF COORDINATION        │
│   Email    [______________]      │
│   Password [______________]      │
│         [  Đăng nhập  ]          │
│   Chưa có tài khoản? Đăng ký     │
└─────────────────────────────────┘
```

### 4. Victim — Dashboard
```
┌───────────────────────────────────────────────────┐
│ RELIEF  | 🆘 Xin chào, [Tên]          [Đăng xuất]  │
├───────────────────────────────────────────────────┤
│  [ + Tạo yêu cầu mới ]                             │
│                                                     │
│  Yêu cầu của tôi:                                  │
│  ┌─────────────────────────────────────────────┐  │
│  │ #12  Thực phẩm | Khẩn cấp | SENT      [Xem] │  │
│  │ #08  Y tế      | Cao      | ACCEPTED  [Xem] │  │
│  │ #03  Nơi ở     | TB       | COMPLETED [Xác  │  │
│  │                                        nhận] │  │
│  └─────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────┘
```

### 5. Victim — Tạo yêu cầu mới
```
┌───────────────────────────────────────┐
│  Tạo yêu cầu cứu trợ                  │
├───────────────────────────────────────┤
│  Địa điểm      [______________]       │
│  [📍 Lấy vị trí GPS hiện tại]         │
│  Loại nhu cầu  [▼ Thực phẩm      ]    │
│  Số người cần  [___]                  │
│  Mức độ khẩn   [▼ Trung bình     ]    │
│  Mô tả         [______________]       │
│       [ Hủy ]      [ Gửi yêu cầu ]    │
└───────────────────────────────────────┘
```

### 6. Victim — Chi tiết yêu cầu
```
┌───────────────────────────────────────┐
│  Yêu cầu #12          Trạng thái: SENT│
├───────────────────────────────────────┤
│  Địa điểm:     123 Lê Lợi, Q1          │
│  Loại nhu cầu: Thực phẩm               │
│  Khẩn cấp:     Cao                     │
│  Rescuer:      (chưa có)               │
│   [ Sửa ]   [ Hủy yêu cầu ]            │
└───────────────────────────────────────┘
```

### 7. Rescuer — Dashboard (tìm/lọc yêu cầu)
```
┌─────────────────────────────────────────────────────┐
│ RELIEF  | 🚑 [Tên]                    [Đăng xuất]    │
├─────────────────────────────────────────────────────┤
│  Tìm: [____________]  Lọc: [▼ Loại] [▼ Khẩn cấp]    │
│  ┌───────────────────────────────────────────────┐  │
│  │ #12 Thực phẩm | Cao     | SENT     [Nhận việc]│  │
│  │ #15 Y tế      | Khẩn cấp| SENT     [Nhận việc]│  │
│  │ #08 Y tế      | Cao     | ACCEPTED [Xem/Cập   │  │
│  │                                     nhật]      │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 8. Rescuer — Chi tiết yêu cầu đã nhận
```
┌───────────────────────────────────────┐
│  Yêu cầu #08       Trạng thái: ACCEPTED│
├───────────────────────────────────────┤
│  Victim:       Nguyễn Văn A            │
│  Địa điểm:     ...  [Xem bản đồ]       │
│  Cập nhật trạng thái:                 │
│  [▼ PROCESSING             ]          │
│       [ Lưu ]  [ Đánh dấu hoàn thành ] │
└───────────────────────────────────────┘
```

### 9. Admin — Dashboard
```
┌─────────────────────────────────────────────────────┐
│ RELIEF ADMIN                          [Đăng xuất]    │
├─────────────────────────────────────────────────────┤
│  Thống kê:                                           │
│  Tổng yêu cầu: 23 | Đã xong: 10 | Người đã giúp: 40  │
│  Quản lý tài khoản:                                  │
│  ┌───────────────────────────────────────────────┐  │
│  │ Nguyễn A  | Rescuer | Active   [Khóa]         │  │
│  │ Trần B    | Victim  | Active   [Khóa]         │  │
│  │ Lê C      | Rescuer | Locked   [Mở khóa]      │  │
│  └───────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────┘
```

### Luồng tổng
Landing → Đăng ký → Login → Dashboard theo role → (Victim) tạo yêu cầu → (Rescuer) nhận/xử lý → Victim xác nhận → Admin theo dõi thống kê + quản lý tài khoản xuyên suốt.