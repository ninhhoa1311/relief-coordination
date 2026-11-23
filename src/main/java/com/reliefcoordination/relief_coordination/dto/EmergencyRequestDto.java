package com.reliefcoordination.relief_coordination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO cho form yêu cầu khẩn cấp (SOS) — không yêu cầu đăng nhập.
 * Chỉ gồm tên, SĐT, và tọa độ GPS tùy chọn.
 */
public class EmergencyRequestDto {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    // Regex cho phép 10 chữ số bắt đầu bằng 0, hoặc +84 theo sau 9 chữ số
    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
        regexp = "^(0[3-9]\\d{8}|\\+84[3-9]\\d{8})$",
        message = "Số điện thoại không hợp lệ (ví dụ: 0912345678 hoặc +84912345678)"
    )
    private String phoneNumber;

    // Tọa độ GPS tùy chọn — null nếu người dùng từ chối hoặc lỗi
    private Double latitude;
    private Double longitude;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
