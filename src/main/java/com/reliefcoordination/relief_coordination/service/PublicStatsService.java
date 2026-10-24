package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.dto.PublicStatsDto;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PublicStatsService {

    private final ReliefRequestRepository reliefRequestRepository;

    public PublicStatsService(ReliefRequestRepository reliefRequestRepository) {
        this.reliefRequestRepository = reliefRequestRepository;
    }

    /**
     * Lấy số liệu thống kê công khai cho trang Landing:
     * - Tổng yêu cầu đã tiếp nhận (tất cả các trạng thái)
     * - Yêu cầu đã hoàn thành (COMPLETED + CONFIRMED)
     * - Tổng số người đã được hỗ trợ (tổng numberOfPeople của COMPLETED + CONFIRMED)
     * 
     * Không lộ bất kỳ thông tin cá nhân nào.
     * Nếu không có dữ liệu trả về 0, không lỗi.
     */
    public PublicStatsDto getPublicStats() {
        try {
            List<Object[]> rows = reliefRequestRepository.countRequestsAndPeopleByStatus();
            if (rows == null || rows.isEmpty()) {
                return PublicStatsDto.empty();
            }

            long totalRequests = 0;
            long completedRequests = 0;
            long totalPeopleSupported = 0;

            for (Object[] row : rows) {
                if (row == null || row.length < 2) {
                    continue;
                }
                RequestStatus status = (RequestStatus) row[0];
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                long people = (row.length > 2 && row[2] != null) ? ((Number) row[2]).longValue() : 0L;

                totalRequests += count;

                if (status == RequestStatus.COMPLETED || status == RequestStatus.CONFIRMED) {
                    completedRequests += count;
                    totalPeopleSupported += people;
                }
            }

            return new PublicStatsDto(totalRequests, completedRequests, totalPeopleSupported);
        } catch (Exception e) {
            return PublicStatsDto.empty();
        }
    }
}
