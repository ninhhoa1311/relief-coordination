package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.dto.ReliefRequestForm;
import com.reliefcoordination.relief_coordination.exception.ResourceNotFoundException;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VictimRequestService {

    private final ReliefRequestRepository reliefRequestRepository;

    public VictimRequestService(ReliefRequestRepository reliefRequestRepository) {
        this.reliefRequestRepository = reliefRequestRepository;
    }

    /** Lấy danh sách yêu cầu của Victim hiện tại. */
    @Transactional(readOnly = true)
    public List<ReliefRequest> danhSachCuaToi(Victim victim) {
        return reliefRequestRepository.findByVictim(victim);
    }

    /** Lấy chi tiết yêu cầu, kiểm tra quyền sở hữu. */
    @Transactional(readOnly = true)
    public ReliefRequest xemChiTiet(Long id, Victim victim) {
        ReliefRequest request = reliefRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu #" + id));
        if (!request.getVictim().getId().equals(victim.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem yêu cầu này");
        }
        return request;
    }

    /** Tạo yêu cầu mới, gán victim, lưu vào DB. */
    public ReliefRequest taoYeuCau(Victim victim, ReliefRequestForm form) {
        ReliefRequest request = new ReliefRequest();
        request.setVictim(victim);
        applyForm(request, form);
        return reliefRequestRepository.save(request);
    }

    /** Sửa yêu cầu, chỉ khi còn SENT. */
    public ReliefRequest suaYeuCau(Long id, Victim victim, ReliefRequestForm form) {
        ReliefRequest request = xemChiTiet(id, victim);
        if (request.getStatus() != RequestStatus.SENT) {
            throw new IllegalStateException("Chỉ được sửa yêu cầu khi trạng thái là SENT");
        }
        applyForm(request, form);
        return reliefRequestRepository.save(request);
    }

    /** Hủy yêu cầu, chỉ khi còn SENT. */
    public void huyYeuCau(Long id, Victim victim) {
        ReliefRequest request = xemChiTiet(id, victim);
        if (request.getStatus() != RequestStatus.SENT) {
            throw new IllegalStateException("Chỉ được hủy yêu cầu khi trạng thái là SENT");
        }
        request.changeStatus(RequestStatus.CANCELLED);
    }

    /** Xác nhận đã nhận hỗ trợ, chỉ khi COMPLETED. */
    public void xacNhanDaNhan(Long id, Victim victim) {
        ReliefRequest request = xemChiTiet(id, victim);
        if (request.getStatus() != RequestStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ được xác nhận khi yêu cầu đã hoàn thành (COMPLETED)");
        }
        request.changeStatus(RequestStatus.CONFIRMED);
    }

    // --- helper ---

    private void applyForm(ReliefRequest request, ReliefRequestForm form) {
        request.setLocation(form.getLocation());
        request.setNeedTypes(form.getNeedTypes() != null ? new java.util.HashSet<>(form.getNeedTypes()) : new java.util.HashSet<>());
        request.setNumberOfPeople(form.getNumberOfPeople());
        request.setUrgencyLevel(form.getUrgencyLevel());
        request.setDescription(form.getDescription());
        request.setLatitude(form.getLatitude());
        request.setLongitude(form.getLongitude());
    }
}
