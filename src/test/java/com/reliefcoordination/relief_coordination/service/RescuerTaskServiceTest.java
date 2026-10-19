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
        ReliefRequest request = new ReliefRequest();
        request.setVictim(victim);
        request.setLocation("123 Duong Le Loi");
        request.setNeedType(NeedType.FOOD);
        request.setNumberOfPeople(4);
        request.setUrgencyLevel(UrgencyLevel.HIGH);
        request.setDescription("Can ho tro khan cap");
        request.changeStatus(RequestStatus.ACCEPTED);
        request.setRescuer(rescuer);
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
        request.setNeedType(NeedType.WATER);
        request.setNumberOfPeople(2);
        request.setUrgencyLevel(UrgencyLevel.MEDIUM);
        request.setDescription("Can nuoc uong khan cap");
        // Gán rescuer trực tiếp, status vẫn là SENT (mặc định)
        request.setRescuer(rescuer);
        return reliefRequestRepository.save(request);
    }
}
