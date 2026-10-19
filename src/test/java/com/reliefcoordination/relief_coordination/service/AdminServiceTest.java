package com.reliefcoordination.relief_coordination.service;

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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private VictimRepository victimRepository;

    @Autowired
    private ReliefRequestRepository reliefRequestRepository;

    // -----------------------------------------------------------------------
    // toggleKhoa — Victim
    // -----------------------------------------------------------------------

    @Test
    void toggleKhoa_victim_khoa_enabled_chuyen_thanh_false() {
        Victim victim = createVictim("admin_toggle_victim@test.com");
        // Mặc định enabled = true
        assertThat(victim.isEnabled()).isTrue();

        adminService.toggleKhoa("victim", victim.getId());

        Victim reloaded = victimRepository.findById(victim.getId()).orElseThrow();
        assertThat(reloaded.isEnabled()).isFalse();
    }

    @Test
    void toggleKhoa_victim_mo_khoa_enabled_chuyen_thanh_true() {
        Victim victim = createVictim("admin_toggle_victim_unlock@test.com");
        victim.setEnabled(false);
        victimRepository.save(victim);

        adminService.toggleKhoa("victim", victim.getId());

        Victim reloaded = victimRepository.findById(victim.getId()).orElseThrow();
        assertThat(reloaded.isEnabled()).isTrue();
    }

    // -----------------------------------------------------------------------
    // toggleKhoa — loai không hợp lệ phải ném lỗi
    // -----------------------------------------------------------------------

    @Test
    void toggleKhoa_loai_admin_phai_nem_IllegalArgumentException() {
        // Bất kỳ id nào — Service phải từ chối ngay tại switch default
        assertThrows(IllegalArgumentException.class, () ->
                adminService.toggleKhoa("admin", 1L)
        );
    }

    @Test
    void toggleKhoa_loai_bat_hop_le_phai_nem_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                adminService.toggleKhoa("superuser", 1L)
        );
    }

    // -----------------------------------------------------------------------
    // huyYeuCau — CONFIRMED không được hủy
    // -----------------------------------------------------------------------

    @Test
    void huyYeuCau_request_da_CONFIRMED_phai_nem_IllegalStateException() {
        Victim victim = createVictim("admin_cancel_confirmed@test.com");
        ReliefRequest request = createRequest(victim);

        // Đưa request về CONFIRMED qua đường chuyển trạng thái hợp lệ
        request.changeStatus(RequestStatus.ACCEPTED);
        request.changeStatus(RequestStatus.PROCESSING);
        request.changeStatus(RequestStatus.COMPLETED);
        request.changeStatus(RequestStatus.CONFIRMED);
        reliefRequestRepository.save(request);

        assertThrows(IllegalStateException.class, () ->
                adminService.huyYeuCau(request.getId())
        );
    }

    @Test
    void huyYeuCau_request_da_CANCELLED_phai_nem_IllegalStateException() {
        Victim victim = createVictim("admin_cancel_cancelled@test.com");
        ReliefRequest request = createRequest(victim);

        request.changeStatus(RequestStatus.CANCELLED);
        reliefRequestRepository.save(request);

        assertThrows(IllegalStateException.class, () ->
                adminService.huyYeuCau(request.getId())
        );
    }

    @Test
    void huyYeuCau_request_SENT_thanh_cong() {
        Victim victim = createVictim("admin_cancel_sent@test.com");
        ReliefRequest request = createRequest(victim);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.SENT);

        adminService.huyYeuCau(request.getId());

        ReliefRequest reloaded = reliefRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.CANCELLED);
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

    private ReliefRequest createRequest(Victim victim) {
        ReliefRequest request = new ReliefRequest();
        request.setVictim(victim);
        request.setLocation("123 Duong Le Loi");
        request.setNeedType(NeedType.FOOD);
        request.setNumberOfPeople(3);
        request.setUrgencyLevel(UrgencyLevel.HIGH);
        request.setDescription("Test request");
        return reliefRequestRepository.save(request);
    }
}
