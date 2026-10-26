package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.exception.ResourceNotFoundException;
import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * Phân trang danh sách nhiệm vụ của Rescuer kết hợp lọc và sắp xếp (nhận tham số dạng chuỗi thô).
     */
    @Transactional(readOnly = true)
    public Page<ReliefRequest> timKiemNhiemVuTheoString(Rescuer rescuer,
                                                        String statusStr,
                                                        String urgencyLevelStr,
                                                        String needTypeStr,
                                                        String location,
                                                        String sortStr,
                                                        int page) {
        if (rescuer == null) {
            throw new IllegalArgumentException("Rescuer không được null");
        }
        RequestStatus status = parseEnum(RequestStatus.class, statusStr);
        UrgencyLevel urgencyLevel = parseEnum(UrgencyLevel.class, urgencyLevelStr);
        NeedType needType = parseEnum(NeedType.class, needTypeStr);
        return timKiemNhiemVu(rescuer, status, urgencyLevel, needType, location, sortStr, page);
    }

    @Transactional(readOnly = true)
    public Page<ReliefRequest> timKiemNhiemVu(Rescuer rescuer,
                                              RequestStatus status,
                                              UrgencyLevel urgencyLevel,
                                              NeedType needType,
                                              String location,
                                              String sortStr,
                                              int page) {
        if (rescuer == null) {
            throw new IllegalArgumentException("Rescuer không được null");
        }
        String cleanLocation = (location != null && !location.isBlank()) ? location.trim() : null;
        String sort = normalizeSortNhiemVu(sortStr);

        int pageNumber = Math.max(0, page);
        Pageable pageable = PageRequest.of(pageNumber, 10);

        return switch (sort) {
            case "accepted_asc" -> reliefRequestRepository.timKiemNhiemVuAcceptedAsc(rescuer, status, needType, urgencyLevel, cleanLocation, pageable);
            default             -> reliefRequestRepository.timKiemNhiemVuAcceptedDesc(rescuer, status, needType, urgencyLevel, cleanLocation, pageable);
        };
    }

    public String normalizeSortNhiemVu(String sortStr) {
        if (sortStr == null) {
            return "accepted_desc";
        }
        String s = sortStr.trim().toLowerCase();
        return switch (s) {
            case "accepted_asc" -> s;
            default -> "accepted_desc";
        };
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    @Transactional(readOnly = true)
    public long demNhiemVuDangLam(Rescuer rescuer) {
        if (rescuer == null) return 0;
        return reliefRequestRepository.countByRescuerAndStatusIn(rescuer, List.of(RequestStatus.ACCEPTED, RequestStatus.PROCESSING));
    }

    @Transactional(readOnly = true)
    public long demNhiemVuHoanThanh(Rescuer rescuer) {
        if (rescuer == null) return 0;
        return reliefRequestRepository.countByRescuerAndStatusIn(rescuer, List.of(RequestStatus.COMPLETED, RequestStatus.CONFIRMED));
    }

    @Transactional(readOnly = true)
    public long demTongNhiemVu(Rescuer rescuer) {
        if (rescuer == null) return 0;
        return reliefRequestRepository.countByRescuer(rescuer);
    }
}
