package com.reliefcoordination.relief_coordination.util;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import org.springframework.stereotype.Component;

@Component
public class EnumLabels {

    public String needType(NeedType type) {
        if (type == null) return "";
        return switch (type) {
            case FOOD -> "Thực phẩm";
            case WATER -> "Nước uống";
            case MEDICAL -> "Y tế";
            case SHELTER -> "Chỗ ở";
            case RESCUE -> "Cứu hộ";
            case OTHER -> "Khác";
        };
    }

    public String urgency(UrgencyLevel level) {
        if (level == null) return "";
        return switch (level) {
            case LOW -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH -> "Cao";
            case CRITICAL -> "Khẩn cấp";
        };
    }

    public String status(RequestStatus status) {
        if (status == null) return "";
        return switch (status) {
            case SENT -> "Đã gửi";
            case ACCEPTED -> "Đã tiếp nhận";
            case PROCESSING -> "Đang xử lý";
            case COMPLETED -> "Hoàn thành";
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
        };
    }
}