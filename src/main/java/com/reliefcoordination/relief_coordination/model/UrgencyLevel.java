package com.reliefcoordination.relief_coordination.model;

public enum UrgencyLevel {
    LOW("Thấp", "Chưa nguy hiểm, có thể chờ vài ngày"),
    MEDIUM("Trung bình", "Đang thiếu thốn, cần hỗ trợ trong ngày"),
    HIGH("Cao", "Có nguy cơ về sức khỏe (trẻ nhỏ, người già, người bệnh), cần trong vài giờ"),
    CRITICAL("Khẩn cấp", "Nguy hiểm đến tính mạng, cần cứu ngay");

    private final String label;
    private final String moTa;

    UrgencyLevel(String label, String moTa) {
        this.label = label;
        this.moTa = moTa;
    }

    public String getLabel() {
        return label;
    }

    public String getMoTa() {
        return moTa;
    }
}