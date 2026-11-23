package com.reliefcoordination.relief_coordination.util;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import org.springframework.stereotype.Component;

@Component
public class EnumLabels {

    public String needType(NeedType type) {
        if (type == null)
            return "";
        return type.getLabel();
    }

    public String urgency(UrgencyLevel level) {
        if (level == null)
            return "";
        return level.getLabel();
    }

    public String status(RequestStatus status) {
        if (status == null)
            return "";
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