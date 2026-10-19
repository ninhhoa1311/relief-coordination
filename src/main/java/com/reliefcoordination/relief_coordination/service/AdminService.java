package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.exception.ResourceNotFoundException;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.User;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import com.reliefcoordination.relief_coordination.repository.RescuerRepository;
import com.reliefcoordination.relief_coordination.repository.VictimRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AdminService {

    private final ReliefRequestRepository reliefRequestRepository;
    private final VictimRepository victimRepository;
    private final RescuerRepository rescuerRepository;

    public AdminService(ReliefRequestRepository reliefRequestRepository,
                        VictimRepository victimRepository,
                        RescuerRepository rescuerRepository) {
        this.reliefRequestRepository = reliefRequestRepository;
        this.victimRepository = victimRepository;
        this.rescuerRepository = rescuerRepository;
    }

    // -----------------------------------------------------------------------
    // Thống kê
    // -----------------------------------------------------------------------

    /** Tập hợp số liệu thống kê cho trang Dashboard Admin. */
    public record AdminStats(
            long tong,
            long daDGui,
            long daTiepNhan,
            long dangXuLy,
            long hoanThanh,
            long daXacNhan,
            long daHuy,
            long soNguoiDaNhanHoTro
    ) {}

    /**
     * Tính toán thống kê:
     * - count theo từng RequestStatus (6 trạng thái)
     * - soNguoiDaNhanHoTro = tổng numberOfPeople của các request CONFIRMED (sum trong Java)
     */
    @Transactional(readOnly = true)
    public AdminStats thongKe() {
        long tong        = reliefRequestRepository.count();
        long daDGui      = reliefRequestRepository.countByStatus(RequestStatus.SENT);
        long daTiepNhan  = reliefRequestRepository.countByStatus(RequestStatus.ACCEPTED);
        long dangXuLy    = reliefRequestRepository.countByStatus(RequestStatus.PROCESSING);
        long hoanThanh   = reliefRequestRepository.countByStatus(RequestStatus.COMPLETED);
        long daXacNhan   = reliefRequestRepository.countByStatus(RequestStatus.CONFIRMED);
        long daHuy       = reliefRequestRepository.countByStatus(RequestStatus.CANCELLED);

        long soNguoiDaNhanHoTro = reliefRequestRepository.findByStatus(RequestStatus.CONFIRMED)
                .stream()
                .mapToLong(r -> r.getNumberOfPeople() != null ? r.getNumberOfPeople() : 0L)
                .sum();

        return new AdminStats(tong, daDGui, daTiepNhan, dangXuLy,
                hoanThanh, daXacNhan, daHuy, soNguoiDaNhanHoTro);
    }

    // -----------------------------------------------------------------------
    // Quản lý tài khoản
    // -----------------------------------------------------------------------

    /** Gộp danh sách Victim + Rescuer (không gồm Admin). */
    @Transactional(readOnly = true)
    public List<User> danhSachTaiKhoan() {
        List<User> users = new ArrayList<>();
        users.addAll(victimRepository.findAll());
        users.addAll(rescuerRepository.findAll());
        return users;
    }

    /**
     * Đảo ngược trạng thái enabled của Victim hoặc Rescuer.
     * loai phải là "victim" hoặc "rescuer" — KHÔNG chấp nhận "admin" hay giá trị khác.
     */
    public void toggleKhoa(String loai, Long id) {
        User user = switch (loai) {
            case "victim"  -> victimRepository.findById(id)
                    .map(v -> (User) v)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Victim #" + id));
            case "rescuer" -> rescuerRepository.findById(id)
                    .map(r -> (User) r)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rescuer #" + id));
            default -> throw new IllegalArgumentException(
                    "Loại tài khoản không hợp lệ: '" + loai + "'. Chỉ chấp nhận 'victim' hoặc 'rescuer'.");
        };
        user.setEnabled(!user.isEnabled());
        // JPA sẽ tự flush và lưu vì entity đang được quản lý trong transaction
    }

    // -----------------------------------------------------------------------
    // Quản lý yêu cầu
    // -----------------------------------------------------------------------

    /** Lấy toàn bộ danh sách yêu cầu cứu trợ. */
    @Transactional(readOnly = true)
    public List<ReliefRequest> danhSachYeuCau() {
        return reliefRequestRepository.findAll();
    }

    /**
     * Admin hủy yêu cầu.
     * Không được hủy nếu status là CONFIRMED hoặc đã là CANCELLED.
     */
    public void huyYeuCau(Long id) {
        ReliefRequest request = reliefRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu #" + id));

        if (request.getStatus() == RequestStatus.CONFIRMED
                || request.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Không thể hủy yêu cầu có trạng thái: " + request.getStatus());
        }

        request.changeStatus(RequestStatus.CANCELLED);
    }
}
