package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.model.Admin;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.User;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import com.reliefcoordination.relief_coordination.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ReliefRequestRepository reliefRequestRepository;

    public AdminService(UserRepository userRepository, ReliefRequestRepository reliefRequestRepository) {
        this.userRepository = userRepository;
        this.reliefRequestRepository = reliefRequestRepository;
    }

    public DashboardStats getDashboardStats() {
        long total = reliefRequestRepository.count();
        long waiting = reliefRequestRepository.countByStatus(RequestStatus.SENT);
        long accepted = reliefRequestRepository.countByStatus(RequestStatus.ACCEPTED);
        long processing = reliefRequestRepository.countByStatus(RequestStatus.PROCESSING);
        long completed = reliefRequestRepository.countByStatus(RequestStatus.COMPLETED);
        long confirmed = reliefRequestRepository.countByStatus(RequestStatus.CONFIRMED);
        long cancelled = reliefRequestRepository.countByStatus(RequestStatus.CANCELLED);
        long peopleHelped = reliefRequestRepository.findByStatus(RequestStatus.CONFIRMED).stream()
                .mapToInt(request -> request.getNumberOfPeople() == null ? 0 : request.getNumberOfPeople())
                .sum();
        return new DashboardStats(
                total,
                waiting,
                accepted + processing,
                completed,
                confirmed,
                cancelled,
                peopleHelped);
    }

    public List<UserAccountView> listAccounts() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(user -> new UserAccountView(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        roleLabel(user),
                        !user.isEnabled(),
                        user instanceof Admin))
                .toList();
    }

    public List<ReliefRequest> listRequests() {
        return reliefRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public ReliefRequest getRequest(Long id) {
        return reliefRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu."));
    }

    public boolean canCancel(ReliefRequest request) {
        return request.getStatus().canTransitionTo(RequestStatus.CANCELLED);
    }

    @Transactional
    public boolean toggleLock(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (user instanceof Admin) {
            throw new IllegalStateException("Không thể khóa Admin");
        }
        user.setEnabled(!user.isEnabled());
        return !user.isEnabled();
    }

    @Transactional
    public void cancelRequest(Long id) {
        ReliefRequest request = getRequest(id);
        request.changeStatus(RequestStatus.CANCELLED);
    }

    private static String roleLabel(User user) {
        if (user instanceof Admin) {
            return "Admin";
        }
        if (user instanceof Rescuer) {
            return "Rescuer";
        }
        return "Victim";
    }

    public record DashboardStats(
            long totalRequests,
            long waitingCount,
            long processingCount,
            long completedCount,
            long confirmedCount,
            long cancelledCount,
            long peopleHelped) {
    }

    public record UserAccountView(
            Long id,
            String fullName,
            String email,
            String role,
            boolean locked,
            boolean admin) {
    }
}
