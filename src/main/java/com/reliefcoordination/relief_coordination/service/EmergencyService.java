package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.dto.EmergencyRequestDto;
import com.reliefcoordination.relief_coordination.model.*;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý yêu cầu khẩn cấp (SOS) không cần đăng nhập.
 */
@Service
@Transactional
public class EmergencyService {

    // Trạng thái cuối — yêu cầu có SĐT trùng ở các trạng thái này được tạo lại bình thường
    private static final List<RequestStatus> STATUSES_CUOI =
            List.of(RequestStatus.CONFIRMED, RequestStatus.CANCELLED);

    private final ReliefRequestRepository reliefRequestRepository;

    public EmergencyService(ReliefRequestRepository reliefRequestRepository) {
        this.reliefRequestRepository = reliefRequestRepository;
    }

    /**
     * Kiểm tra xem số điện thoại đã có yêu cầu SOS đang hoạt động chưa.
     * Trả về true nếu đã tồn tại (spam), false nếu có thể tạo mới.
     */
    @Transactional(readOnly = true)
    public boolean daCoYeuCauDangHoatDong(String phoneNumber) {
        return reliefRequestRepository.existsByGuestPhoneAndStatusNotIn(phoneNumber, STATUSES_CUOI);
    }

    /**
     * Tạo yêu cầu SOS khẩn cấp:
     * - victim = null (khách vãng lai)
     * - urgencyLevel = CRITICAL (cao nhất)
     * - needType = RESCUE (cứu người)
     * - status = SENT (mặc định)
     * - GPS tùy chọn: nếu có lat/lng thì lưu, nếu không thì để null
     */
    public ReliefRequest taoYeuCauSOS(EmergencyRequestDto dto) {
        ReliefRequest request = new ReliefRequest();

        // Victim = null — yêu cầu SOS từ khách vãng lai
        request.setVictim(null);
        request.setGuestName(dto.getFullName());
        request.setGuestPhone(dto.getPhoneNumber());

        // Mặc định khẩn cấp cao nhất và loại "cứu người"
        request.setUrgencyLevel(UrgencyLevel.CRITICAL);
        request.setNeedTypes(java.util.Set.of(NeedType.RESCUE));
        request.setNumberOfPeople(1);

        // Địa điểm placeholder khi chưa có GPS text
        request.setLocation("Chưa xác định (yêu cầu SOS)");
        request.setDescription("Yêu cầu khẩn cấp (SOS) - liên hệ qua số điện thoại: " + dto.getPhoneNumber());

        // GPS tùy chọn — không bắt buộc, không chặn form nếu bị từ chối
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            request.setLatitude(dto.getLatitude());
            request.setLongitude(dto.getLongitude());
            request.setLocation("Tọa độ GPS (yêu cầu SOS)");
        }

        return reliefRequestRepository.save(request);
    }
}
