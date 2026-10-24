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
     * Tính toán thống kê toàn hệ thống:
     * - Dùng 1 câu query GROUP BY lấy số lượng theo từng RequestStatus và tổng số người được cứu (CONFIRMED).
     */
    @Transactional(readOnly = true)
    public AdminStats thongKe() {
        List<Object[]> rows = reliefRequestRepository.countRequestsAndPeopleByStatus();
        long tong = 0;
        long daDGui = 0;
        long daTiepNhan = 0;
        long dangXuLy = 0;
        long hoanThanh = 0;
        long daXacNhan = 0;
        long daHuy = 0;
        long soNguoiDaNhanHoTro = 0;

        for (Object[] row : rows) {
            RequestStatus status = (RequestStatus) row[0];
            long count = ((Number) row[1]).longValue();
            long people = row[2] != null ? ((Number) row[2]).longValue() : 0L;

            tong += count;
            switch (status) {
                case SENT -> daDGui = count;
                case ACCEPTED -> daTiepNhan = count;
                case PROCESSING -> dangXuLy = count;
                case COMPLETED -> hoanThanh = count;
                case CONFIRMED -> {
                    daXacNhan = count;
                    soNguoiDaNhanHoTro = people;
                }
                case CANCELLED -> daHuy = count;
            }
        }

        return new AdminStats(tong, daDGui, daTiepNhan, dangXuLy,
                hoanThanh, daXacNhan, daHuy, soNguoiDaNhanHoTro);
    }

    // -----------------------------------------------------------------------
    // Tập hợp ID của các Rescuer bị Admin chủ động khóa (để phân biệt với Chờ duyệt khi enabled = false)
    private final java.util.Set<Long> lockedRescuerIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // -----------------------------------------------------------------------
    // Quản lý tài khoản
    // -----------------------------------------------------------------------

    /** Lấy danh sách Victim. */
    @Transactional(readOnly = true)
    public List<Victim> danhSachVictim() {
        return victimRepository.findAll();
    }

    /** Lấy danh sách Rescuer. */
    @Transactional(readOnly = true)
    public List<Rescuer> danhSachRescuer() {
        return rescuerRepository.findAll();
    }

    /** Gộp danh sách Victim + Rescuer (không gồm Admin). */
    @Transactional(readOnly = true)
    public List<User> danhSachTaiKhoan() {
        List<User> users = new ArrayList<>();
        users.addAll(victimRepository.findAll());
        users.addAll(rescuerRepository.findAll());
        return users;
    }

    /** Kiểm tra Rescuer có đang ở trạng thái Chờ duyệt hay không. */
    public boolean isPending(Rescuer rescuer) {
        return !rescuer.isEnabled() && !lockedRescuerIds.contains(rescuer.getId());
    }

    /** Kiểm tra Rescuer có đang ở trạng thái Bị khóa hay không. */
    public boolean isLocked(Rescuer rescuer) {
        return !rescuer.isEnabled() && lockedRescuerIds.contains(rescuer.getId());
    }

    /** Lấy mã trạng thái cho Rescuer: PENDING (Chờ duyệt), ACTIVE (Hoạt động), LOCKED (Bị khóa). */
    public String getRescuerStatus(Rescuer rescuer) {
        if (rescuer.isEnabled()) {
            return "ACTIVE";
        }
        if (lockedRescuerIds.contains(rescuer.getId())) {
            return "LOCKED";
        }
        return "PENDING";
    }

    /** Đếm số Rescuer đang chờ duyệt. */
    @Transactional(readOnly = true)
    public long countRescuerChoDuyet() {
        return rescuerRepository.findAll().stream()
                .filter(this::isPending)
                .count();
    }

    /** Duyệt 1 Rescuer đang chờ duyệt. */
    public void duyetRescuer(Long id) {
        Rescuer rescuer = rescuerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rescuer #" + id));
        rescuer.setEnabled(true);
        lockedRescuerIds.remove(id);
    }

    /** Duyệt hàng loạt mọi Rescuer đang chờ duyệt, bỏ qua những tài khoản đang bị khóa. */
    public int duyetTatCaRescuer() {
        List<Rescuer> pendingRescuers = rescuerRepository.findAll().stream()
                .filter(this::isPending)
                .toList();

        for (Rescuer r : pendingRescuers) {
            r.setEnabled(true);
            lockedRescuerIds.remove(r.getId());
        }
        return pendingRescuers.size();
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

        if (user instanceof Rescuer r) {
            if (r.isEnabled()) {
                r.setEnabled(false);
                lockedRescuerIds.add(r.getId());
            } else {
                r.setEnabled(true);
                lockedRescuerIds.remove(r.getId());
            }
        } else {
            user.setEnabled(!user.isEnabled());
        }
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
     * Lọc và phân trang yêu cầu cứu trợ cho Admin.
     * Sắp xếp: chưa xong lên trước (ưu tiên theo mức khẩn cấp giảm dần), đã xong/hủy xuống sau, mới nhất trước.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReliefRequest> timKiemYeuCauAdmin(
            RequestStatus status,
            com.reliefcoordination.relief_coordination.model.UrgencyLevel urgencyLevel,
            com.reliefcoordination.relief_coordination.model.NeedType needType,
            String keyword,
            org.springframework.data.domain.Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return reliefRequestRepository.timKiemYeuCauAdmin(status, urgencyLevel, needType, cleanKeyword, pageable);
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

    // -----------------------------------------------------------------------
    // Chi tiết Victim & Yêu cầu của Victim
    // -----------------------------------------------------------------------

    public record VictimRequestStats(
            long total,
            long sent,
            long accepted,
            long processing,
            long completed,
            long confirmed,
            long cancelled
    ) {}

    /**
     * Tìm Victim theo id, ném ResourceNotFoundException nếu không tìm thấy hoặc không phải Victim.
     */
    @Transactional(readOnly = true)
    public Victim timVictimTheoId(Long id) {
        return victimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Victim #" + id));
    }

    /**
     * Thống kê số lượng yêu cầu theo từng trạng thái của 1 Victim bằng 1 câu GROUP BY duy nhất.
     */
    @Transactional(readOnly = true)
    public VictimRequestStats thongKeYeuCauCuaVictim(Long victimId) {
        // Đảm bảo victim tồn tại
        timVictimTheoId(victimId);

        List<Object[]> rows = reliefRequestRepository.countRequestsByStatusForVictim(victimId);
        long sent = 0;
        long accepted = 0;
        long processing = 0;
        long completed = 0;
        long confirmed = 0;
        long cancelled = 0;

        for (Object[] row : rows) {
            RequestStatus status = (RequestStatus) row[0];
            long count = ((Number) row[1]).longValue();
            switch (status) {
                case SENT -> sent = count;
                case ACCEPTED -> accepted = count;
                case PROCESSING -> processing = count;
                case COMPLETED -> completed = count;
                case CONFIRMED -> confirmed = count;
                case CANCELLED -> cancelled = count;
            }
        }
        long total = sent + accepted + processing + completed + confirmed + cancelled;
        return new VictimRequestStats(total, sent, accepted, processing, completed, confirmed, cancelled);
    }

    /**
     * Lấy danh sách yêu cầu của Victim có phân trang, tùy chọn lọc theo trạng thái.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReliefRequest> danhSachYeuCauCuaVictim(
            Long victimId,
            RequestStatus status,
            org.springframework.data.domain.Pageable pageable) {
        // Đảm bảo victim tồn tại
        timVictimTheoId(victimId);

        if (status != null) {
            return reliefRequestRepository.findByVictimIdAndStatus(victimId, status, pageable);
        }
        return reliefRequestRepository.findByVictimId(victimId, pageable);
    }
}
