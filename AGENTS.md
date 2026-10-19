# Luật dự án Relief Coordination

Đọc chi tiết trong `.cursor/rules/project.mdc` và `docs/SPEC.md` trước khi làm bất kỳ việc gì — hai file đó là nguồn luật chính của dự án, áp dụng cho mọi công cụ AI (Cursor, Antigravity).

Tóm tắt nhanh:
- Stack: Spring Boot 4.1.1, Java 21, Maven, Spring Security, Spring Data JPA, Thymeleaf, PostgreSQL (Neon), Bootstrap 5 CDN.
- Kiến trúc: controller -> service -> repository -> model.
- Mỗi role một controller, dùng @RequestMapping("/role") ở mức class.
- Chuyển trạng thái ReliefRequest CHỈ qua request.changeStatus(...), không bao giờ setStatus trực tiếp (trừ DataInitializer khi seed).
- Phân quyền kiểm tra cả role và quyền sở hữu dữ liệu trong Service.
- Không đọc, ghi hay commit file .env. Không hard-code mật khẩu hay connection string.
- Không thêm dependency vào pom.xml, không sửa entity trong model/, SecurityConfig, layout.html khi chưa hỏi nhóm trưởng.
- Test dùng @Transactional để tự rollback, không thêm H2 hay dependency test mới.
- Sau mỗi thay đổi, chạy ./mvnw clean install -DskipTests và báo kết quả.