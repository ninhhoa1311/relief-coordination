package com.reliefcoordination.relief_coordination.util;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;

/** Nhãn tiếng Việt cho các enum, dùng trong template qua T(...EnumLabels). */
public final class EnumLabels {

    private EnumLabels() {
    }

    public static String need(NeedType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case FOOD -> "Thực phẩm";
            case WATER -> "Nước uống";
            case MEDICAL -> "Y tế";
            case SHELTER -> "Chỗ ở";
            case RESCUE -> "Cứu người";
            case OTHER -> "Khác";
        };
    }

    public static String urgency(UrgencyLevel level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case LOW -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH -> "Cao";
            case CRITICAL -> "Khẩn cấp";
        };
    }
}
