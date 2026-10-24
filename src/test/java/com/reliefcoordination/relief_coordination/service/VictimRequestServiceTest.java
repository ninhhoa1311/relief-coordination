package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.dto.ReliefRequestForm;
import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
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
class VictimRequestServiceTest {

    @Autowired
    private VictimRequestService victimRequestService;

    @Autowired
    private ReliefRequestRepository reliefRequestRepository;

    @Autowired
    private VictimRepository victimRepository;

    @Test
    void taoYeuCau_thanhCong_statusMacDinhLaSent() {
        Victim victim = createVictim("victim_tao@test.com");
        ReliefRequestForm form = createSampleForm();

        ReliefRequest created = victimRequestService.taoYeuCau(victim, form);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(RequestStatus.SENT);
        assertThat(created.getVictim().getId()).isEqualTo(victim.getId());
        assertThat(created.getLocation()).isEqualTo("123 Duong Le Loi");
        assertThat(created.getNumberOfPeople()).isEqualTo(4);
    }

    @Test
    void victimAKhongXemDuocRequestCuaVictimB_throwsAccessDeniedException() {
        Victim victimA = createVictim("victimA_view@test.com");
        Victim victimB = createVictim("victimB_view@test.com");
        ReliefRequest requestB = victimRequestService.taoYeuCau(victimB, createSampleForm());

        assertThrows(AccessDeniedException.class, () -> {
            victimRequestService.xemChiTiet(requestB.getId(), victimA);
        });
    }

    @Test
    void victimAKhongSuaDuocRequestCuaVictimB_throwsAccessDeniedException() {
        Victim victimA = createVictim("victimA_edit@test.com");
        Victim victimB = createVictim("victimB_edit@test.com");
        ReliefRequest requestB = victimRequestService.taoYeuCau(victimB, createSampleForm());

        ReliefRequestForm updateForm = createSampleForm();
        updateForm.setLocation("Dia diem moi");

        assertThrows(AccessDeniedException.class, () -> {
            victimRequestService.suaYeuCau(requestB.getId(), victimA, updateForm);
        });
    }

    @Test
    void khongSuaDuocKhiStatusKhacSent_throwsIllegalStateException() {
        Victim victim = createVictim("victim_status_edit@test.com");
        ReliefRequest request = victimRequestService.taoYeuCau(victim, createSampleForm());

        // Chuyển sang CANCELLED
        request.changeStatus(RequestStatus.CANCELLED);
        reliefRequestRepository.save(request);

        ReliefRequestForm updateForm = createSampleForm();
        updateForm.setLocation("Dia diem sua");

        assertThrows(IllegalStateException.class, () -> {
            victimRequestService.suaYeuCau(request.getId(), victim, updateForm);
        });
    }

    @Test
    void suaYeuCau_thanhCongKhiStatusSent() {
        Victim victim = createVictim("victim_edit_ok@test.com");
        ReliefRequest request = victimRequestService.taoYeuCau(victim, createSampleForm());

        ReliefRequestForm updateForm = createSampleForm();
        updateForm.setLocation("456 Tran Hung Dao");
        updateForm.setNumberOfPeople(10);

        ReliefRequest updated = victimRequestService.suaYeuCau(request.getId(), victim, updateForm);

        assertThat(updated.getLocation()).isEqualTo("456 Tran Hung Dao");
        assertThat(updated.getNumberOfPeople()).isEqualTo(10);
    }

    @Test
    void huyYeuCau_thanhCongKhiStatusSent() {
        Victim victim = createVictim("victim_cancel_ok@test.com");
        ReliefRequest request = victimRequestService.taoYeuCau(victim, createSampleForm());

        victimRequestService.huyYeuCau(request.getId(), victim);

        ReliefRequest reloaded = reliefRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    void xacNhanDaNhan_thanhCongKhiStatusCompleted() {
        Victim victim = createVictim("victim_confirm_ok@test.com");
        ReliefRequest request = victimRequestService.taoYeuCau(victim, createSampleForm());
        request.changeStatus(RequestStatus.ACCEPTED);
        request.changeStatus(RequestStatus.PROCESSING);
        request.changeStatus(RequestStatus.COMPLETED);
        reliefRequestRepository.save(request);

        victimRequestService.xacNhanDaNhan(request.getId(), victim);

        ReliefRequest reloaded = reliefRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
    }

    @Test
    void xacNhanDaNhan_thatBaiKhiStatusKhacCompleted() {
        Victim victim = createVictim("victim_confirm_fail@test.com");
        ReliefRequest request = victimRequestService.taoYeuCau(victim, createSampleForm());

        assertThrows(IllegalStateException.class, () -> {
            victimRequestService.xacNhanDaNhan(request.getId(), victim);
        });
    }

    // --- Helpers ---

    private Victim createVictim(String email) {
        Victim victim = new Victim();
        victim.setFullName("Nguyen Van Test");
        victim.setEmail(email);
        victim.setPassword("encoded_secret");
        victim.setPhoneNumber("0987654321");
        return victimRepository.save(victim);
    }

    private ReliefRequestForm createSampleForm() {
        ReliefRequestForm form = new ReliefRequestForm();
        form.setLocation("123 Duong Le Loi");
        form.setNeedTypes(java.util.Set.of(NeedType.FOOD));
        form.setNumberOfPeople(4);
        form.setUrgencyLevel(UrgencyLevel.HIGH);
        form.setDescription("Can thuc pham gap cho 4 nguoi");
        form.setLatitude(10.7769);
        form.setLongitude(106.7009);
        return form;
    }
}
