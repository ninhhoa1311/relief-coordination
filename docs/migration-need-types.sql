-- ==============================================================================
-- Migration: Chuyển đổi Loại nhu cầu (NeedType) sang bảng quan hệ nhiều-nhiều
-- Áp dụng cho: PostgreSQL (Neon Database)
--
-- QUAN TRỌNG:
-- 1. Chạy script này trên TỪNG branch Neon PostgreSQL (branch cá nhân, branch main/demo)
--    SAU KHI ứng dụng đã khởi động ít nhất một lần để Hibernate tự động tạo bảng
--    'relief_request_need_types'.
-- 2. Đảm bảo chạy đúng thứ tự: copy dữ liệu trước, drop cột cũ sau.
-- ==============================================================================

-- BƯỚC 1: Sao chép dữ liệu cũ từ cột relief_requests.need_type sang bảng mới relief_request_need_types
-- Có điều kiện kiểm tra để tránh chèn trùng lặp nếu script được chạy lại nhiều lần.
INSERT INTO relief_request_need_types (request_id, need_type)
SELECT id, need_type
FROM relief_requests
WHERE need_type IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 
    FROM relief_request_need_types 
    WHERE relief_request_need_types.request_id = relief_requests.id 
      AND relief_request_need_types.need_type = relief_requests.need_type
  );

-- BƯỚC 2: Xóa cột need_type cũ khỏi bảng relief_requests
-- Giúp tránh lỗi NOT NULL constraint vi phạm khi insert dữ liệu mới và dọn dẹp DB sạch sẽ.
ALTER TABLE relief_requests DROP COLUMN IF EXISTS need_type;
