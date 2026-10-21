package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import org.springframework.stereotype.Component;

/** Nhãn tiếng Việt và màu badge cho trang Rescuer; dùng trong template qua {@code @rescuerViewLabels}. */
@Component("rescuerViewLabels")
public class RescuerViewLabels {

    public String need(NeedType type) {
        return switch (type) {
            case FOOD -> "Thực phẩm";
            case WATER -> "Nước uống";
            case MEDICAL -> "Y tế";
            case SHELTER -> "Chỗ ở";
            case RESCUE -> "Cứu người";
            case OTHER -> "Khác";
        };
    }

    public String urgency(UrgencyLevel level) {
        return switch (level) {
            case LOW -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH -> "Cao";
            case CRITICAL -> "Khẩn cấp";
        };
    }

    /** Màu badge theo SPEC: SENT xám, ACCEPTED xanh dương, PROCESSING vàng, COMPLETED xanh lá nhạt, CONFIRMED xanh lá, CANCELLED đỏ. */
    public String statusBadge(RequestStatus status) {
        return switch (status) {
            case SENT -> "text-bg-secondary";
            case ACCEPTED -> "text-bg-primary";
            case PROCESSING -> "text-bg-warning";
            case COMPLETED -> "bg-success-subtle text-success-emphasis";
            case CONFIRMED -> "text-bg-success";
            case CANCELLED -> "text-bg-danger";
        };
    }

    /** Chữ trên nút cho bước hợp lệ kế tiếp; rỗng nếu không có bước nào. */
    public String nextAction(RequestStatus current) {
        return switch (current) {
            case ACCEPTED -> "Bắt đầu xử lý";
            case PROCESSING -> "Đánh dấu hoàn thành";
            default -> "";
        };
    }
}