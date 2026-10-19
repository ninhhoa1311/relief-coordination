package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.exception.ResourceNotFoundException;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RescuerTaskService {

    private final ReliefRequestRepository reliefRequestRepository;

    public RescuerTaskService(ReliefRequestRepository reliefRequestRepository) {
        this.reliefRequestRepository = reliefRequestRepository;
    }

    /** Lấy danh sách nhiệm vụ đã nhận của Rescuer hiện tại. */
    @Transactional(readOnly = true)
    public List<ReliefRequest> nhiemVuCuaToi(Rescuer rescuer) {
        return reliefRequestRepository.findByRescuer(rescuer);
    }

    /**
     * Xem chi tiết một nhiệm vụ. Kiểm tra quyền sở hữu:
     * rescuer phải là người đã được gán cho request này.
     */
    @Transactional(readOnly = true)
    public ReliefRequest xemChiTiet(Long id, Rescuer rescuer) {
        ReliefRequest request = reliefRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhiệm vụ #" + id));
        kiemTraQuyen(request, rescuer);
        return request;
    }

    /**
     * Server tự tính bước kế tiếp dựa trên trạng thái hiện tại:
     *   ACCEPTED   → PROCESSING
     *   PROCESSING → COMPLETED
     * Các trạng thái khác: ném IllegalStateException.
     * KHÔNG nhận tham số trạng thái đích từ bên ngoài.
     */
    public ReliefRequest capNhatTrangThaiTiep(Long id, Rescuer rescuer) {
        ReliefRequest request = reliefRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhiệm vụ #" + id));
        kiemTraQuyen(request, rescuer);

        RequestStatus buocTiep = switch (request.getStatus()) {
            case ACCEPTED   -> RequestStatus.PROCESSING;
            case PROCESSING -> RequestStatus.COMPLETED;
            default -> throw new IllegalStateException(
                    "Không có bước tiếp theo hợp lệ cho trạng thái: " + request.getStatus());
        };

        request.changeStatus(buocTiep);
        return reliefRequestRepository.save(request);
    }

    // --- helper ---

    private void kiemTraQuyen(ReliefRequest request, Rescuer rescuer) {
        if (request.getRescuer() == null
                || !request.getRescuer().getId().equals(rescuer.getId())) {
            throw new AccessDeniedException(
                    "Bạn không có quyền thao tác với nhiệm vụ #" + request.getId());
        }
    }
}
