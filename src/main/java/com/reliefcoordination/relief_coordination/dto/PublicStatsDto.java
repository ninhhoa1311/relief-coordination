package com.reliefcoordination.relief_coordination.dto;

/**
 * DTO chứa số liệu thống kê công khai cho trang Landing.
 * Chỉ gồm các số liệu tổng hợp, không chứa thông tin cá nhân.
 */
public record PublicStatsDto(
        long totalRequests,
        long completedRequests,
        long totalPeopleSupported
) {
    public static PublicStatsDto empty() {
        return new PublicStatsDto(0L, 0L, 0L);
    }
}
