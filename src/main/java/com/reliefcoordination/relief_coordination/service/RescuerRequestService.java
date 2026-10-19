package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.exception.ResourceNotFoundException;
import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RescuerRequestService {

    private final ReliefRequestRepository reliefRequestRepository;

    // Gán số thứ tự ưu tiên thủ công: CRITICAL > HIGH > MEDIUM > LOW
    // UrgencyLevel khai báo: LOW=0, MEDIUM=1, HIGH=2, CRITICAL=3
    // Dùng Map để rõ ràng, không phụ thuộc ordinal()
    private static final Map<UrgencyLevel, Integer> URGENCY_PRIORITY = Map.of(
            UrgencyLevel.CRITICAL, 3,
            UrgencyLevel.HIGH,     2,
            UrgencyLevel.MEDIUM,   1,
            UrgencyLevel.LOW,      0
    );

    public RescuerRequestService(ReliefRequestRepository reliefRequestRepository) {
        this.reliefRequestRepository = reliefRequestRepository;
    }

    /**
     * Lấy danh sách yêu cầu đang chờ (SENT), sắp xếp theo mức khẩn cấp giảm dần.
     */
    @Transactional(readOnly = true)
    public List<ReliefRequest> danhSachDangCho() {
        return reliefRequestRepository.findByStatus(RequestStatus.SENT)
                .stream()
                .sorted(Comparator.comparingInt(
                        (ReliefRequest r) -> URGENCY_PRIORITY.getOrDefault(r.getUrgencyLevel(), 0)
                ).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm / lọc trên danh sách SENT theo location (contains, không hoa thường),
     * needType và urgencyLevel (bỏ qua nếu null).
     */
    @Transactional(readOnly = true)
    public List<ReliefRequest> timKiem(String location, NeedType needType, UrgencyLevel urgencyLevel) {
        return danhSachDangCho().stream()
                .filter(r -> location == null || location.isBlank()
                        || r.getLocation().toLowerCase().contains(location.toLowerCase()))
                .filter(r -> needType == null || r.getNeedType() == needType)
                .filter(r -> urgencyLevel == null || r.getUrgencyLevel() == urgencyLevel)
                .collect(Collectors.toList());
    }

    /**
     * Xem chi tiết yêu cầu theo id. Ném ResourceNotFoundException nếu không tìm thấy.
     * Không kiểm tra thông tin Victim ở đây — ẩn tên/SĐT xử lý ở tầng template.
     */
    @Transactional(readOnly = true)
    public ReliefRequest xemChiTiet(Long id) {
        return reliefRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu #" + id));
    }

    /**
     * Rescuer nhận nhiệm vụ.
     * Kiểm tra status == SENT, rồi chuyển ACCEPTED và gán rescuer.
     * @Version trên entity tự phát hiện xung đột nếu 2 người cùng lưu.
     */
    public ReliefRequest nhanNhiemVu(Long id, Rescuer rescuer) {
        ReliefRequest request = reliefRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu #" + id));

        if (request.getStatus() != RequestStatus.SENT) {
            throw new IllegalStateException(
                    "Yêu cầu #" + id + " không còn ở trạng thái SENT (hiện: " + request.getStatus() + ")");
        }

        request.changeStatus(RequestStatus.ACCEPTED);
        request.setRescuer(rescuer);
        return reliefRequestRepository.save(request);
    }
}
