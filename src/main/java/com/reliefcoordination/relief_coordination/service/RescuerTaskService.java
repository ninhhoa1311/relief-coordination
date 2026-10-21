package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.User;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import com.reliefcoordination.relief_coordination.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Nghiệp vụ "Nhiệm vụ của tôi" của Rescuer: xem nhiệm vụ đã nhận và đẩy trạng thái từng bước. */
@Service
public class RescuerTaskService {

    private final UserRepository userRepository;
    private final ReliefRequestRepository reliefRequestRepository;

    public RescuerTaskService(UserRepository userRepository,
                              ReliefRequestRepository reliefRequestRepository) {
        this.userRepository = userRepository;
        this.reliefRequestRepository = reliefRequestRepository;
    }

    /** Các yêu cầu mà Rescuer này đã nhận, mới nhất lên trước. */
    @Transactional(readOnly = true)
    public List<ReliefRequest> getMyTasks(String email) {
        Rescuer rescuer = findRescuer(email);
        return reliefRequestRepository.findByRescuer(rescuer).stream()
                .sorted(Comparator.comparing(ReliefRequest::getCreatedAt).reversed())
                .toList();
    }

    /** Chi tiết một nhiệm vụ; chỉ Rescuer đã nhận yêu cầu đó mới xem được. */
    @Transactional(readOnly = true)
    public ReliefRequest getMyTask(Long id, String email) {
        Rescuer rescuer = findRescuer(email);
        return loadOwnedTask(id, rescuer);
    }

    /**
     * Đẩy nhiệm vụ sang bước kế tiếp. Trạng thái đích do server tự tính từ trạng thái hiện tại,
     * không nhận từ client.
     */
    @Transactional
    public ReliefRequest advanceStatus(Long id, String email) {
        Rescuer rescuer = findRescuer(email);
        ReliefRequest request = loadOwnedTask(id, rescuer);

        RequestStatus next = nextStatus(request.getStatus())
                .orElseThrow(() -> new IllegalStateException(
                        "Nhiệm vụ ở trạng thái " + request.getStatus() + " không còn bước cập nhật nào"));

        request.changeStatus(next);
        return reliefRequestRepository.saveAndFlush(request);
    }

    /** Bước hợp lệ kế tiếp mà Rescuer được phép thực hiện; rỗng nếu không còn bước nào. */
    public Optional<RequestStatus> nextStatus(RequestStatus current) {
        return switch (current) {
            case ACCEPTED -> Optional.of(RequestStatus.PROCESSING);
            case PROCESSING -> Optional.of(RequestStatus.COMPLETED);
            default -> Optional.empty();
        };
    }

    private ReliefRequest loadOwnedTask(Long id, Rescuer rescuer) {
        ReliefRequest request = reliefRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy nhiệm vụ"));

        if (request.getRescuer() == null
                || !request.getRescuer().getId().equals(rescuer.getId())) {
            throw new AccessDeniedException("Bạn không phải người nhận nhiệm vụ này");
        }
        return request;
    }

    private Rescuer findRescuer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản"));
        if (!(user instanceof Rescuer rescuer)) {
            throw new AccessDeniedException("Tài khoản không phải Rescuer");
        }
        return rescuer;
    }
}