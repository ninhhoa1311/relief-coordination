# Tài liệu dự án — Cần cứu trợ

**Điều hướng:** [README chính](../README.md)

---

## Danh sách tài liệu

| File | Mô tả |
|---|---|
| [SPEC.md](SPEC.md) | Đặc tả yêu cầu: mục tiêu, vai trò, chức năng, quy tắc nghiệp vụ, quyết định đã chốt |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Kiến trúc phân tầng controller→service→repository→model, luồng xử lý request, bảo mật, template |
| [OOP-DESIGN.md](OOP-DESIGN.md) | Thiết kế OOP: kế thừa, đóng gói, đa hình, trừu tượng — có ví dụ code thật và sơ đồ lớp |
| [DATABASE.md](DATABASE.md) | Cấu trúc bảng, quan hệ, chiến lược kế thừa JOINED, sơ đồ ER |
| [WORKFLOW.md](WORKFLOW.md) | Sơ đồ trạng thái yêu cầu, sơ đồ tuần tự các luồng chính |
| [ROUTES.md](ROUTES.md) | Toàn bộ route: phương thức, đường dẫn, role truy cập, mô tả |
| [USER-GUIDE.md](USER-GUIDE.md) | Hướng dẫn sử dụng từng bước cho Khách, Victim, Rescuer, Admin |
| [TESTING.md](TESTING.md) | Danh sách test tự động, cách chạy, kịch bản kiểm thử thủ công |

---

## File migration SQL

| File | Mô tả |
|---|---|
| [migration-need-types.sql](migration-need-types.sql) | Chuyển `need_type` (cột đơn) sang `relief_request_need_types` (bảng nhiều-nhiều) |

