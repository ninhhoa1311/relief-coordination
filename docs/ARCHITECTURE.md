Điều phối cứu trợ — kiến ​​trúc hiện tại (sau Bước 0)

Spring Boot hỗ trợ thiên tai: Nạn nhân gửi yêu cầu, Người cứu hộ nhận và xử lý, Quản trị viên quản trị. This domain, JPA, security, data sample, 6 control, trang Landing, menu theo vai trò và 3 trang chủ. Chưa có tầng dịch vụ, đăng ký tài khoản, hay CRUD yêu cầu: đó là công việc của Bước 1.

Đang có mã mô tả tài liệu này . Yêu cầu nghiệp vụ và giao diện nằm trong docs/SPEC.md.

1. Phiên bản và phụ thuộc
Java 21, Spring Boot 4.1.1, Maven, artifact relief-coordination 0.0.1-SNAPSHOT.
Chính: spring-boot-starter-webmvc, data-jpa, security, thymeleaf, validation.
Khác: thymeleaf-extras-springsecurity6PostgreSQL (môi trường chạy), Lombok, DevTools (tùy chọn).
Kiểm tra: các khởi động *-testtương ứng (JPA, Security, Thymeleaf, Validation, WebMVC).
Không thêm phần phụ thuộc vào pom.xmlkhi chưa hỏi trưởng nhóm.
2. Cấu hình
Cổng 8080. Kết nối cơ sở dữ liệu đọc từ môi trường biến, tải qua spring.config.importtệp .env(tùy chọn, không cam kết; mẫu ở .env.example).
JPA: ddl-auto=update, open-in-view=false, show-sqlbật, format_sqlbật. Không còn spring.jpa.database-platform(Ngủ đông tự động nhận phương ngữ từ URL JDBC).
Lá xạ hương: cache.
Cơ sở dữ liệu: PostgreSQL trên Neon. Mỗi thành viên sử dụng một nhánh riêng của Neon, không dùng chung nhánh production.
Đăng nhập khởi động có một cảnh báo AuthenticationProvider bean ... UserDetailsService beans will not be used. Đây là chủ ý (đã tự khai báo DaoAuthenticationProvider), không cần sửa.
3. Gói cây và lớp trò chơi

:com.reliefcoordination.relief_coordination

ReliefCoordinationApplication: khởi động lại.

cấu hình

SecurityConfig: chuỗi lọc, biểu mẫu đăng nhập, phân quyền theo URL tiền tố, khai báo DaoAuthenticationProvider.
PasswordEncoderConfig: bean BCrypt.
DataInitializer: Seed khi bảng userstrống (xem mục 9).

bộ điều khiển (lớp 6)

AuthController: GET /loginreturn auth/login(trang độc lập, không dùng bố cục).
HomeController: GET /(Hạ cánh cho người chưa đăng nhập, người đã đăng nhập đã chuyển sang /home) và GET /home(chuyển về vai trò chính xác của trang chủ).
VictimController: @RequestMapping("/victim"), hiện có GET /home.
AdminController: @RequestMapping("/admin"), hiện có GET /home.
RescuerRequestController: @RequestMapping("/rescuer"), hiện có GET /home(placeholder). Sẽ chứa danh sách, lọc, chi tiết, nhận yêu cầu.
RescuerTaskController: @RequestMapping("/rescuer"), lớp trống. Sẽ chứa nhiệm vụ của tôi và cập nhật trạng thái.

người mẫu

User(tóm tắt), Admin, Victim, Rescuer, ReliefRequest.
Liệt kê: RequestStatus, NeedType, UrgencyLevel.

kho lưu trữ

UserRepository( findByEmail), AdminRepository, VictimRepository, RescuerRepository.
ReliefRequestRepository: findByStatus, findByVictim, findByRescuer, countByStatus.

bảo vệ

CustomUserDetailsService: nạp người dùng theo email, phân bổ ROLE_ADMIN/ ROLE_RESCUER/ ROLE_VICTIMtheo lớp con, tài khoản enabled = falsebị khóa.

Bài kiểm tra

ReliefCoordinationApplicationTests(chỉ có contextLoads).

Chưa có gói hàng servicevà dto.

4. Mẫu Thymeleaf (9 tập tin)

Thư mục src/main/resources/templates/. CSS/JS: Bootstrap 5.3.3 qua CDN (jsDelivr).

Trang công khai (cài đặt, không sử dụng bố cục):

landing.html: trang chủ cho người chưa đăng nhập, 2 nút dẫn tới /register/victimvà /register/rescuer(hai trang này không tồn tại).
auth/login.html: biểu mẫu đăng nhập.

Bố cục và thực đơn:

fragments/layout.html: navbar trên (thương hiệu con trỏ /home, người dùng tên sec:authentication="name", nút đăng xuất có CSRF), nhúng nội dung bằng th:replace="~{${content} :: content}". Menu hiển thị theo vai trò bằng cách sec:authorizebọc trong th:block, mỗi vai trò nhúng một đoạn riêng. Không có thanh bên. Không thể chỉnh sửa tệp này.
fragments/nav-victim.html, nav-rescuer.html, nav-admin.html: menu của từng vai trò (đoạn tên nav). Vai trò phụ trách nào chỉ chỉnh sửa menu tệp của vai trò đó.

Trang chủ theo vai trò (đoạn content):

victim/home.html, rescuer/home.html, admin/home.html: không có chỗ giữ chỗ. Bộ điều khiển ở contentđây pageTitle.
5. Thực thể, quan hệ, kế thừa

User (abstract) , kế thừa THAM GIA, bảng users: id, fullName, email(duy nhất), password, phoneNumber, enabled(mặc định true).

Quản trị viên/Nạn nhân/Người cứu hộ : chỉ kế thừa User(bảng admins/ victims/ rescuers).

Yêu cầu cứu trợ ( g relief_requests):

Trường: id, victim( @ManyToOnebắt buộc), rescuer( @ManyToOnetùy chọn), location, latitude, longitude(tùy chọn), needType, numberOfPeople, urgencyLevel, description(tối đa 1000), status(mặc định SENT), createdAt, updatedAt, version( @Version).
@PreUpdatecập nhật updatedAt.
Không có @OneToManythác ngược, không có thác nước, không có orphanRemoval.
setStatus đã khóa ( @Setter(AccessLevel.NONE)trên status); không thể gọi từ bên ngoài.

Phương pháp hoàn thành nhiệm vụ trên ReliefRequest:

changeStatus(next): check RequestStatus.canTransitionTo(), sai bước thì Ném IllegalStateException. Chỉ kiểm tra bước đi, không kiểm tra ai đã thay đổi (việc đó nằm ở Dịch vụ).
accept(rescuer): gọi changeStatus(ACCEPTED)trước rồi mới chỉ định rescuer. Use Changes for setRescuer.
isEditable(): true khi trạng thái là SENT(Nạn nhân vẫn được chỉnh sửa/hủy).
hasLocation()và getMapUrl(): kiểm tra có GPS và liên kết trả tiền Google Maps (null if no mode). An toàn với JPA vì thực thể sử dụng quyền truy cập trường.
6. Enum và chuyển trạng thái
NeedType: THỰC PHẨM, NƯỚC UỐNG, Y TẾ, CHỖ Ở, CỨU HỘ, CÁC NHU CẦU KHÁC.
UrgencyLevelMức độ nguy hiểm: THẤP, TRUNG BÌNH, CAO, NGUY HIỂM.
RequestStatusđi đến các bước chuyển tiếp được phép trong mã:
ĐÃ GỬI → ĐÃ CHẤP NHẬN hoặc ĐÃ HỦY
ĐÃ CHẤP NHẬN → ĐANG XỬ LÝ hoặc ĐÃ HỦY
ĐANG XỬ LÝ → HOÀN THÀNH hoặc ĐÃ HỦY
HOÀN THÀNH → ĐÃ XÁC NHẬN
CONFIRMED và CANCELED ở cuối trạng thái.
canTransitionTo(next)chỉ được gọi từ ReliefRequest.changeStatus(). Mọi nơi khác phải đi qua changeStatus()hoặc accept().

Cần nhất: docs/SPEC.md mục 3 cho Quản trị viên được yêu cầu hủy bỏ COMPLETED, nhưng hiện tại không cho COMPLETED → CANCELLED. Phải chọn một: chỉnh sửa canTransitionTo()bước được phép hoặc sửa SPEC để quản trị viên chỉ hủy từ SENT, ACCEPTED, PROCESSING. Chỉ có tệp sửa đổi chính này của nhóm.

7. Điểm cuối hiện có

Bộ điều khiển chỉ có @GetMapping; không có @PostMappingcái nào trong controller/. POST đăng nhập và đăng nhập trong quá trình xử lý Spring Security.

Công khai:

GET /: HomeController, Landing (đã đăng nhập thì chuyển sang /home).
GET /login: AuthController.
POST /loginĐăng nhập bằng biểu mẫu này qua Spring Security.

Cần đăng nhập:

POST /logout: Mặc định mùa xuân, có CSRF.
GET /home: HomeController, chuyển về /victim/home, /rescuer/homehoặc /admin/home.
GET /victim/home: VictimController, vai trò NẠN NHÂN.
GET /rescuer/home: RescuerRequestController, vai trò CỨU HỘ.
GET /admin/home: AdminController, vai trò QUẢN TRỊ VIÊN.

Đã được phép truy cập công khai nhưng chưa có bộ điều khiển (trả 404 hiện tại): /register/**.

8. An ninh
Công khai: /, /login, /register/**, /error, /css/**, /js/**.
Phân quyền theo tiền tố: /victim/**vai trò VICTIM, /rescuer/**vai trò RESCUER, /admin/**vai trò ADMIN. Các URL còn lại cần đăng nhập.
Đăng nhập thành công luôn về /home. Đăng xuất về /login?logout.
CSRF được bật, phiên đăng nhập (không phải JWT).
Xác thực: DaoAuthenticationProvider+ BCrypt + CustomUserDetailsService.
Chỉ nhóm trưởng sửa SecurityConfig.
9. vật liệu mẫu ( DataInitializer)

Run khi bảng userstrống. Tạo 1 Quản trị viên, 2 Người cứu hộ, 3 Nạn nhân (tài khoản và mật khẩu xem docs/SPEC.mdmục 8) và 20 mẫu yêu cầu:

Phân bố trạng thái: GỬI 7, CHẤP NHẬN 3, ĐANG XỬ LÝ 3, HOÀN THÀNH 2, XÁC NHẬN 3, HỦY 2.
Yêu cầu có lẻ thứ tự (1, 3, 5...) có GPS tốc độ.
Seed đi qua accept()và changeStatus(), không còn gọi setStatus.

Muốn gieo lại: chạy TRUNCATE TABLE users RESTART IDENTITY CASCADE;trên nhánh Neon của chính mình rồi chạy lại ứng dụng. Không thể chạy lệnh này trên nhánh production.

10. Đã xong và chưa làm

Đã xong

Model JOINED, enum, ReliefRequestcó changeStatus()/ accept(), repository.
BCrypt, SecurityFilterChain, đăng nhập và đăng xuất có CSRF, khóa tài khoản qua enabled.
Landing tại /, chuyển hướng /hometheo vai trò.
Layout Bootstrap có menu theo vai trò, giữ chỗ 3 trang chủ.
Mẫu dữ liệu đủ mọi trạng thái, có GPS.
Rescuer đã phân tách thành công RescuerRequestControllervà RescuerTaskController.

Chưa làm (B- 1)

Đăng ký Nạn nhân tìm đến Người cứu hộ ( /register/victim, /register/rescuer).
Yêu cầu CRUD của Victim, danh sách / lọc / nhận của Rescuer, cập nhật trạng thái, các màn hình Quản trị viên.
Tầng service(nơi kiểm tra quyền sở hữu, xử lý ObjectOptimisticLockingFailureException, bắt IllegalStateException), và dto/ form has @Valid.
RescuerTaskControllerChưa có ánh xạ nào cả.
Bài kiểm tra chỉ có contextLoads.

khoảng ro

ddl-auto=updatekhông có di chuyển: sửa thực thể phải thông báo nhóm trưởng.
@Versionvới dữ liệu cũ: dòng version = nullsẽ bị lỗi khi cập nhật. Chạy UPDATE relief_requests SET version = 0 WHERE version IS NULL;nếu có.
11. Vị trí mới đặt mã cho Bước 1
Dịch vụ: gói của bạn service, vai trò của tôi là một tệp (xem VictimRequestService, RescuerRequestService, RescuerTaskService, AdminService). Không thể thu thập một Dịch vụ lớn.
Form và DTO: tạo gói dtohoặc form, mỗi chức năng của một tệp, có @Valid. Không liên kết thực thể thẳng vào biểu mẫu.
Bản mẫu: templates/<role>/<tên-trang>.html, bọc nội dung trong <div th:fragment="content">. Trang công khai mới (đăng ký) làm độc lập giống landing.html.
Menu: thêm link vào fragments/nav-<role>.htmlvai trò của mình.
Truy vấn mới: thêm vào cuối ReliefRequestRepository , không sửa phương thức của người khác.
File ghi quyền: pom.xml, SecurityConfig, application.properties, model/, layout.html, DataInitializeredit trưởng nhóm chỉ.