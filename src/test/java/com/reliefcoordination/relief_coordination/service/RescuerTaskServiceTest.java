package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import com.reliefcoordination.relief_coordination.repository.RescuerRepository;
import com.reliefcoordination.relief_coordination.repository.VictimRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class RescuerTaskServiceTest {

    @Autowired
    private RescuerTaskService rescuerTaskService;

    @Autowired
    private ReliefRequestRepository reliefRequestRepository;

    @Autowired
    private RescuerRepository rescuerRepository;

    @Autowired
    private VictimRepository victimRepository;

    // -----------------------------------------------------------------------
    // Test 1: Rescuer A không cập nhật được nhiệm vụ của Rescuer B
    // -----------------------------------------------------------------------

    @Test
    void rescuerAKhongCapNhatDuocNhiemVuCuaRescuerB_throwsAccessDeniedException() {
        Victim victim    = createVictim("task_victim_ab@test.com");
        Rescuer rescuerA = createRescuer("rescuerA_task@test.com");
        Rescuer rescuerB = createRescuer("rescuerB_task@test.com");

        // Tạo request và gán cho Rescuer B (ACCEPTED)
        ReliefRequest request = createAcceptedRequest(victim, rescuerB);

        // Rescuer A cố cập nhật nhiệm vụ của Rescuer B → phải ném AccessDeniedException
        final Long requestId = request.getId();
        assertThrows(AccessDeniedException.class, () ->
                rescuerTaskService.capNhatTrangThaiTiep(requestId, rescuerA)
        );
    }

    // -----------------------------------------------------------------------
    // Test 2: capNhatTrangThaiTiep khi status == SENT phải ném IllegalStateException
    // -----------------------------------------------------------------------

    @Test
    void capNhatTrangThaiTiep_khiStatusLaSent_throwsIllegalStateException() {
        Victim victim   = createVictim("task_victim_sent@test.com");
        Rescuer rescuer = createRescuer("rescuer_sent@test.com");

        // Request vẫn SENT, chưa có rescuer — kiemTraQuyen sẽ ném AccessDeniedException
        // vì request.getRescuer() == null. Để test đúng yêu cầu (IllegalStateException
        // vì không có bước kế tiếp), ta gán rescuer trực tiếp vào DB mà không đổi status.
        ReliefRequest request = createSentRequestWithRescuer(victim, rescuer);

        final Long requestId = request.getId();
        // Status là SENT, không có bước tiếp theo hợp lệ → IllegalStateException
        assertThrows(IllegalStateException.class, () ->
                rescuerTaskService.capNhatTrangThaiTiep(requestId, rescuer)
        );
    }

    // -----------------------------------------------------------------------
    // Test 3: 2 lần gọi đúng thứ tự: ACCEPTED → PROCESSING → COMPLETED
    // -----------------------------------------------------------------------

    @Test
    void capNhatTrangThaiTiep_haiLanLienTiep_chuyenDungThuTu() {
        Victim victim   = createVictim("task_victim_seq@test.com");
        Rescuer rescuer = createRescuer("rescuer_seq@test.com");

        // Tạo request ACCEPTED
        ReliefRequest request = createAcceptedRequest(victim, rescuer);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.ACCEPTED);

        // Lần 1: ACCEPTED → PROCESSING
        ReliefRequest afterFirst = rescuerTaskService.capNhatTrangThaiTiep(request.getId(), rescuer);
        assertThat(afterFirst.getStatus()).isEqualTo(RequestStatus.PROCESSING);

        // Lần 2: PROCESSING → COMPLETED
        ReliefRequest afterSecond = rescuerTaskService.capNhatTrangThaiTiep(request.getId(), rescuer);
        assertThat(afterSecond.getStatus()).isEqualTo(RequestStatus.COMPLETED);

        // Xác nhận trong DB
        ReliefRequest reloaded = reliefRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.COMPLETED);
        assertThat(reloaded.getRescuer().getId()).isEqualTo(rescuer.getId());
    }

    // -----------------------------------------------------------------------
    // Test 4 (bổ sung): xemChiTiet — Rescuer A không xem được nhiệm vụ của Rescuer B
    // -----------------------------------------------------------------------

    @Test
    void xemChiTiet_rescuerSai_throwsAccessDeniedException() {
        Victim victim    = createVictim("task_victim_view@test.com");
        Rescuer rescuerA = createRescuer("rescuerA_view@test.com");
        Rescuer rescuerB = createRescuer("rescuerB_view@test.com");

        ReliefRequest request = createAcceptedRequest(victim, rescuerB);

        final Long requestId = request.getId();
        assertThrows(AccessDeniedException.class, () ->
                rescuerTaskService.xemChiTiet(requestId, rescuerA)
        );
    }

    // -----------------------------------------------------------------------
    // Test 5 (bổ sung): capNhatTrangThaiTiep lần 3 sau COMPLETED phải ném lỗi
    // -----------------------------------------------------------------------

    @Test
    void capNhatTrangThaiTiep_lanBaSauCompleted_throwsIllegalStateException() {
        Victim victim   = createVictim("task_victim_comp@test.com");
        Rescuer rescuer = createRescuer("rescuer_comp@test.com");

        ReliefRequest request = createAcceptedRequest(victim, rescuer);

        // ACCEPTED → PROCESSING → COMPLETED
        rescuerTaskService.capNhatTrangThaiTiep(request.getId(), rescuer);
        rescuerTaskService.capNhatTrangThaiTiep(request.getId(), rescuer);

        // Lần 3: COMPLETED không có bước tiếp → IllegalStateException
        final Long requestId = request.getId();
        assertThrows(IllegalStateException.class, () ->
                rescuerTaskService.capNhatTrangThaiTiep(requestId, rescuer)
        );
    }

    // -----------------------------------------------------------------------
    // Test 6 (mới): Chỉ trả nhiệm vụ của ĐÚNG Rescuer đang đăng nhập
    // -----------------------------------------------------------------------

    @Test
    void timKiemNhiemVu_chiTraNhiemVuCuaDungRescuer() {
        Victim victim = createVictim("task_filter_user@test.com");
        Rescuer rescuerA = createRescuer("rescuerA_own@test.com");
        Rescuer rescuerB = createRescuer("rescuerB_own@test.com");

        ReliefRequest reqA = createAcceptedRequest(victim, rescuerA);
        ReliefRequest reqB = createAcceptedRequest(victim, rescuerB);

        org.springframework.data.domain.Page<ReliefRequest> pageA =
                rescuerTaskService.timKiemNhiemVuTheoString(rescuerA, null, null, null, null, null, 0);

        List<Long> idsA = pageA.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(idsA).contains(reqA.getId());
        assertThat(idsA).doesNotContain(reqB.getId());
    }

    // -----------------------------------------------------------------------
    // Test 7 (mới): Lọc nhiệm vụ theo trạng thái (COMPLETED)
    // -----------------------------------------------------------------------

    @Test
    void timKiemNhiemVu_locTheoTrangThai() {
        Victim victim = createVictim("task_filter_status@test.com");
        Rescuer rescuer = createRescuer("rescuer_status@test.com");

        ReliefRequest reqAccepted = createAcceptedRequest(victim, rescuer);
        ReliefRequest reqCompleted = createAcceptedRequest(victim, rescuer);
        rescuerTaskService.capNhatTrangThaiTiep(reqCompleted.getId(), rescuer); // PROCESSING
        rescuerTaskService.capNhatTrangThaiTiep(reqCompleted.getId(), rescuer); // COMPLETED

        org.springframework.data.domain.Page<ReliefRequest> page =
                rescuerTaskService.timKiemNhiemVuTheoString(rescuer, "COMPLETED", null, null, null, null, 0);

        List<Long> ids = page.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(ids).contains(reqCompleted.getId());
        assertThat(ids).doesNotContain(reqAccepted.getId());
    }

    // -----------------------------------------------------------------------
    // Test 8 (cập nhật): Sắp xếp mặc định: mới nhất trước (theo updatedAt DESC)
    // -----------------------------------------------------------------------

    @Test
    void timKiemNhiemVu_sapXepMacDinh_moiNhatTruoc() {
        Victim victim = createVictim("task_sort_default@test.com");
        Rescuer rescuer = createRescuer("rescuer_sort_def@test.com");

        ReliefRequest reqOld = createAcceptedRequestWithUpdatedAt(
                victim, rescuer, java.time.LocalDateTime.now().minusHours(5));
        ReliefRequest reqNew = createAcceptedRequestWithUpdatedAt(
                victim, rescuer, java.time.LocalDateTime.now().minusHours(1));

        org.springframework.data.domain.Page<ReliefRequest> page =
                rescuerTaskService.timKiemNhiemVuTheoString(rescuer, null, null, null, null, null, 0);

        List<Long> ids = page.getContent().stream().map(ReliefRequest::getId).toList();

        // Mặc định: mới nhất trước — reqNew (updatedAt mới hơn) phải đứng trước reqOld
        assertThat(ids.indexOf(reqNew.getId())).isLessThan(ids.indexOf(reqOld.getId()));
    }

    // -----------------------------------------------------------------------
    // Test 9 (mới): Sắp xếp thời gian nhận mới nhất / cũ nhất
    // -----------------------------------------------------------------------

    @Test
    void timKiemNhiemVu_sapXepThoiGianNhan() {
        Victim victim = createVictim("task_sort_time@test.com");
        Rescuer rescuer = createRescuer("rescuer_sort_time@test.com");

        ReliefRequest reqOld = createAcceptedRequestWithUpdatedAt(
                victim, rescuer, java.time.LocalDateTime.now().minusHours(3));
        ReliefRequest reqNew = createAcceptedRequestWithUpdatedAt(
                victim, rescuer, java.time.LocalDateTime.now().minusHours(1));

        // Mới nhất trước
        org.springframework.data.domain.Page<ReliefRequest> pageDesc =
                rescuerTaskService.timKiemNhiemVuTheoString(rescuer, null, null, null, null, "accepted_desc", 0);
        List<Long> idsDesc = pageDesc.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(idsDesc.indexOf(reqNew.getId())).isLessThan(idsDesc.indexOf(reqOld.getId()));

        // Cũ nhất trước
        org.springframework.data.domain.Page<ReliefRequest> pageAsc =
                rescuerTaskService.timKiemNhiemVuTheoString(rescuer, null, null, null, null, "accepted_asc", 0);
        List<Long> idsAsc = pageAsc.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(idsAsc.indexOf(reqOld.getId())).isLessThan(idsAsc.indexOf(reqNew.getId()));
    }

    // -----------------------------------------------------------------------
    // Test 10 (mới): Nhiệm vụ từ yêu cầu SOS của khách vãng lai (victim = null)
    // -----------------------------------------------------------------------

    @Test
    void timKiemNhiemVu_sosKhach_hoatDongBinhThuong() {
        Rescuer rescuer = createRescuer("rescuer_guest_sos@test.com");

        ReliefRequest guestTask = new ReliefRequest();
        guestTask.setVictim(null);
        guestTask.setGuestName("Khach Can Cuu");
        guestTask.setGuestPhone("0988777666");
        guestTask.setLocation("Cần Thơ");
        guestTask.setNeedTypes(java.util.Set.of(NeedType.FOOD));
        guestTask.setNumberOfPeople(3);
        guestTask.setUrgencyLevel(UrgencyLevel.HIGH);
        guestTask.changeStatus(RequestStatus.ACCEPTED);
        guestTask.setRescuer(rescuer);
        guestTask = reliefRequestRepository.save(guestTask);

        org.springframework.data.domain.Page<ReliefRequest> page =
                rescuerTaskService.timKiemNhiemVuTheoString(rescuer, null, null, null, "Cần Thơ", null, 0);

        List<Long> ids = page.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(ids).contains(guestTask.getId());
    }

    // -----------------------------------------------------------------------
    // Test 11 (mới): Tham số không hợp lệ fallback về mặc định không lỗi 500
    // -----------------------------------------------------------------------

    @Test
    void timKiemNhiemVu_thamSoKhongHopLe_fallbackKhongLoi() {
        Rescuer rescuer = createRescuer("rescuer_invalid_param@test.com");

        org.springframework.data.domain.Page<ReliefRequest> page =
                rescuerTaskService.timKiemNhiemVuTheoString(rescuer, "INVALID_STATUS", "INVALID_URGENCY", "INVALID_NEED", "Ha Noi", "sort_bua_bai", -1);

        assertThat(page).isNotNull();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Victim createVictim(String email) {
        Victim victim = new Victim();
        victim.setFullName("Nguyen Van Test");
        victim.setEmail(email);
        victim.setPassword("encoded_secret");
        victim.setPhoneNumber("0987654321");
        return victimRepository.save(victim);
    }

    private Rescuer createRescuer(String email) {
        Rescuer rescuer = new Rescuer();
        rescuer.setFullName("Tran Thi Rescuer");
        rescuer.setEmail(email);
        rescuer.setPassword("encoded_secret");
        rescuer.setPhoneNumber("0912345678");
        return rescuerRepository.save(rescuer);
    }

    /** Tạo request SENT rồi chuyển sang ACCEPTED và gán rescuer. */
    private ReliefRequest createAcceptedRequest(Victim victim, Rescuer rescuer) {
        return createAcceptedRequestWithUrgency(victim, rescuer, UrgencyLevel.HIGH);
    }

    private ReliefRequest createAcceptedRequestWithUrgency(Victim victim, Rescuer rescuer, UrgencyLevel urgencyLevel) {
        ReliefRequest request = new ReliefRequest();
        request.setVictim(victim);
        request.setLocation("123 Duong Le Loi");
        request.setNeedTypes(java.util.Set.of(NeedType.FOOD));
        request.setNumberOfPeople(4);
        request.setUrgencyLevel(urgencyLevel);
        request.setDescription("Can ho tro khan cap");
        request.changeStatus(RequestStatus.ACCEPTED);
        request.setRescuer(rescuer);
        return reliefRequestRepository.save(request);
    }

    private ReliefRequest createAcceptedRequestWithUpdatedAt(Victim victim, Rescuer rescuer, java.time.LocalDateTime updatedAt) {
        ReliefRequest request = new ReliefRequest();
        request.setVictim(victim);
        request.setLocation("123 Duong Le Loi");
        request.setNeedTypes(java.util.Set.of(NeedType.FOOD));
        request.setNumberOfPeople(4);
        request.setUrgencyLevel(UrgencyLevel.HIGH);
        request.setDescription("Can ho tro khan cap");
        request.changeStatus(RequestStatus.ACCEPTED);
        request.setRescuer(rescuer);
        request.setUpdatedAt(updatedAt);
        return reliefRequestRepository.save(request);
    }

    /**
     * Tạo request SENT nhưng gán rescuer trực tiếp (không đổi status).
     * Dùng để test trường hợp status SENT với rescuer đã được gán.
     */
    private ReliefRequest createSentRequestWithRescuer(Victim victim, Rescuer rescuer) {
        ReliefRequest request = new ReliefRequest();
        request.setVictim(victim);
        request.setLocation("456 Duong Tran Hung Dao");
        request.setNeedTypes(java.util.Set.of(NeedType.WATER));
        request.setNumberOfPeople(2);
        request.setUrgencyLevel(UrgencyLevel.MEDIUM);
        request.setDescription("Can nuoc uong khan cap");
        // Gán rescuer trực tiếp, status vẫn là SENT (mặc định)
        request.setRescuer(rescuer);
        return reliefRequestRepository.save(request);
    }
}
