package com.reliefcoordination.relief_coordination.model;

public enum NeedType {
    FOOD("Thực phẩm"),
    WATER("Nước uống"),
    MEDICAL("Y tế"),
    SHELTER("Nơi ở"),
    RESCUE("Cứu người"),
    OTHER("Khác");

    private final String label;

    NeedType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}