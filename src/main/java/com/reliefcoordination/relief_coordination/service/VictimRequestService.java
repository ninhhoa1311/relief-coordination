package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.dto.VictimRequestForm;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import com.reliefcoordination.relief_coordination.repository.VictimRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nghiệp vụ của Victim: tạo / xem / sửa / hủy / xác nhận yêu cầu cứu trợ.
 * Mọi thao tác trên một yêu cầu đều kiểm tra đúng chủ sở hữu.
 */
@Service
@RequiredArgsConstructor
public class VictimRequestService {

    private final ReliefRequestRepository requestRepo;
    private final VictimRepository victimRepo;

    /** Lấy Victim đang đăng nhập (principal của Spring Security chỉ chứa email). */
    private Victim currentVictim() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Chưa đăng nhập");
        }
        String email = auth.getName();
        return victimRepo.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không phải Victim: " + email));
    }

    /** Tìm yêu cầu theo id và bắt buộc phải là của Victim hiện tại. */
    private ReliefRequest findOwned(Long id) {
        ReliefRequest r = requestRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu #" + id));
        Victim me = currentVictim();
        // So sánh theo id: entity không override equals nên không dùng equals().
        if (!r.getVictim().getId().equals(me.getId())) {
            throw new AccessDeniedException("Bạn không có quyền với yêu cầu này");
        }
        return r;
    }

    private void apply(ReliefRequest r, VictimRequestForm form) {
        r.setLocation(form.getLocation().trim());
        r.setLatitude(form.getLatitude());
        r.setLongitude(form.getLongitude());
        r.setNeedType(form.getNeedType());
        r.setNumberOfPeople(form.getNumberOfPeople());
        r.setUrgencyLevel(form.getUrgencyLevel());
        r.setDescription(form.getDescription());
    }

    /** Danh sách yêu cầu của Victim đang đăng nhập (mới nhất trước). */
    @Transactional(readOnly = true)
    public List<ReliefRequest> listByVictim() {
        return requestRepo.findByVictimOrderByCreatedAtDesc(currentVictim());
    }

    /** Tạo yêu cầu mới, trạng thái mặc định SENT. */
    @Transactional
    public ReliefRequest create(VictimRequestForm form) {
        ReliefRequest r = new ReliefRequest();
        r.setVictim(currentVictim());
        apply(r, form);
        return requestRepo.save(r);
    }

    /** Xem chi tiết — chỉ chủ sở hữu mới xem được. */
    @Transactional(readOnly = true)
    public ReliefRequest getDetail(Long id) {
        return findOwned(id);
    }

    /** Sửa yêu cầu — chỉ khi đúng chủ sở hữu VÀ status == SENT. */
    @Transactional
    public ReliefRequest update(Long id, VictimRequestForm form) {
        ReliefRequest r = findOwned(id);
        if (!r.isEditable()) {
            throw new IllegalStateException("Chỉ được sửa yêu cầu khi chưa có ai nhận");
        }
        apply(r, form);
        return requestRepo.save(r);
    }

    /** Hủy yêu cầu — chỉ khi đúng chủ sở hữu VÀ status == SENT. */
    @Transactional
    public void cancel(Long id) {
        ReliefRequest r = findOwned(id);
        if (r.getStatus() != RequestStatus.SENT) {
            throw new IllegalStateException("Chỉ được hủy yêu cầu khi chưa có ai nhận");
        }
        r.changeStatus(RequestStatus.CANCELLED);
        requestRepo.save(r);
    }

    /** Xác nhận đã nhận hỗ trợ — chỉ khi đúng chủ sở hữu VÀ status == COMPLETED. */
    @Transactional
    public void confirm(Long id) {
        ReliefRequest r = findOwned(id);
        if (r.getStatus() != RequestStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ xác nhận được khi yêu cầu đã hoàn thành");
        }
        r.changeStatus(RequestStatus.CONFIRMED);
        requestRepo.save(r);
    }
}
