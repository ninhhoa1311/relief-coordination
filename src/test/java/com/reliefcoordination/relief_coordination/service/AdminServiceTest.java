package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import com.reliefcoordination.relief_coordination.repository.VictimRepository;
import java.util.List;
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
    private com.reliefcoordination.relief_coordination.repository.RescuerRepository rescuerRepository;

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
    // duyetRescuer & duyetTatCaRescuer
    // -----------------------------------------------------------------------

    @Test
    void duyetRescuer_thanhCong_chuyen_enabled_thanh_true() {
        com.reliefcoordination.relief_coordination.model.Rescuer rescuer = createRescuer("rescuer_duyet@test.com", false);
        assertThat(adminService.isPending(rescuer)).isTrue();

        adminService.duyetRescuer(rescuer.getId());

        com.reliefcoordination.relief_coordination.model.Rescuer reloaded = rescuerRepository.findById(rescuer.getId()).orElseThrow();
        assertThat(reloaded.isEnabled()).isTrue();
        assertThat(adminService.isPending(reloaded)).isFalse();
    }

    @Test
    void duyetTatCaRescuer_thanhCong_va_khong_mo_khoa_rescuer_bi_khoa() {
        com.reliefcoordination.relief_coordination.model.Rescuer r1 = createRescuer("pending1@test.com", false);
        com.reliefcoordination.relief_coordination.model.Rescuer r2 = createRescuer("pending2@test.com", false);
        com.reliefcoordination.relief_coordination.model.Rescuer r3 = createRescuer("active_to_lock@test.com", true);

        // Khóa r3 -> r3 thành Bị khóa (LOCKED)
        adminService.toggleKhoa("rescuer", r3.getId());
        assertThat(adminService.isLocked(r3)).isTrue();
        assertThat(adminService.isPending(r3)).isFalse();

        assertThat(adminService.isPending(r1)).isTrue();
        assertThat(adminService.isPending(r2)).isTrue();

        int approved = adminService.duyetTatCaRescuer();
        assertThat(approved).isGreaterThanOrEqualTo(2);

        // r1, r2 được duyệt
        com.reliefcoordination.relief_coordination.model.Rescuer reloaded1 = rescuerRepository.findById(r1.getId()).orElseThrow();
        com.reliefcoordination.relief_coordination.model.Rescuer reloaded2 = rescuerRepository.findById(r2.getId()).orElseThrow();
        assertThat(reloaded1.isEnabled()).isTrue();
        assertThat(reloaded2.isEnabled()).isTrue();

        // r3 vẫn bị khóa
        com.reliefcoordination.relief_coordination.model.Rescuer reloaded3 = rescuerRepository.findById(r3.getId()).orElseThrow();
        assertThat(reloaded3.isEnabled()).isFalse();
        assertThat(adminService.isLocked(reloaded3)).isTrue();
    }

    // -----------------------------------------------------------------------
    // timVictimTheoId, thongKeYeuCauCuaVictim & danhSachYeuCauCuaVictim
    // -----------------------------------------------------------------------

    @Test
    void timVictimTheoId_tonTai_traVeVictim() {
        Victim victim = createVictim("victim_find_ok@test.com");
        Victim found = adminService.timVictimTheoId(victim.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(victim.getId());
        assertThat(found.getEmail()).isEqualTo("victim_find_ok@test.com");
    }

    @Test
    void timVictimTheoId_khongTonTai_throwsResourceNotFoundException() {
        assertThrows(com.reliefcoordination.relief_coordination.exception.ResourceNotFoundException.class, () ->
                adminService.timVictimTheoId(999999L)
        );
    }

    @Test
    void timVictimTheoId_idLaRescuer_throwsResourceNotFoundException() {
        com.reliefcoordination.relief_coordination.model.Rescuer rescuer = createRescuer("rescuer_not_victim@test.com", true);
        assertThrows(com.reliefcoordination.relief_coordination.exception.ResourceNotFoundException.class, () ->
                adminService.timVictimTheoId(rescuer.getId())
        );
    }

    @Test
    void thongKeYeuCauCuaVictim_chinhXac() {
        Victim victim = createVictim("victim_stats@test.com");

        // Tạo 2 request SENT, 1 ACCEPTED
        ReliefRequest req1 = createRequest(victim);
        ReliefRequest req2 = createRequest(victim);

        ReliefRequest req3 = createRequest(victim);
        com.reliefcoordination.relief_coordination.model.Rescuer rescuer = createRescuer("rescuer_stats@test.com", true);
        req3.accept(rescuer);
        reliefRequestRepository.save(req3);

        AdminService.VictimRequestStats stats = adminService.thongKeYeuCauCuaVictim(victim.getId());
        assertThat(stats.total()).isEqualTo(3);
        assertThat(stats.sent()).isEqualTo(2);
        assertThat(stats.accepted()).isEqualTo(1);
        assertThat(stats.completed()).isEqualTo(0);
    }

    @Test
    void danhSachYeuCauCuaVictim_phanTrangVaLocStatus() {
        Victim victim = createVictim("victim_page@test.com");
        ReliefRequest req1 = createRequest(victim);
        ReliefRequest req2 = createRequest(victim);

        ReliefRequest req3 = createRequest(victim);
        com.reliefcoordination.relief_coordination.model.Rescuer rescuer = createRescuer("rescuer_page@test.com", true);
        req3.accept(rescuer);
        reliefRequestRepository.save(req3);

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending());

        // Lọc tất cả
        org.springframework.data.domain.Page<ReliefRequest> allPage =
                adminService.danhSachYeuCauCuaVictim(victim.getId(), null, pageable);
        assertThat(allPage.getTotalElements()).isEqualTo(3);

        // Lọc theo SENT
        org.springframework.data.domain.Page<ReliefRequest> sentPage =
                adminService.danhSachYeuCauCuaVictim(victim.getId(), RequestStatus.SENT, pageable);
        assertThat(sentPage.getTotalElements()).isEqualTo(2);

        // Lọc theo ACCEPTED
        org.springframework.data.domain.Page<ReliefRequest> acceptedPage =
                adminService.danhSachYeuCauCuaVictim(victim.getId(), RequestStatus.ACCEPTED, pageable);
        assertThat(acceptedPage.getTotalElements()).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // timKiemYeuCauAdmin — Lọc động, sắp xếp và thống kê toàn hệ thống
    // -----------------------------------------------------------------------

    @Test
    void thongKe_toanHeThong_dungGroupByQuery() {
        Victim victim = createVictim("victim_thongke_sys@test.com");
        Rescuer rescuer = createRescuer("rescuer_thongke_sys@test.com", true);

        // Tạo 1 CONFIRMED với 5 người
        ReliefRequest reqConfirmed = createRequest(victim);
        reqConfirmed.setNumberOfPeople(5);
        reqConfirmed.accept(rescuer);
        reqConfirmed.changeStatus(RequestStatus.PROCESSING);
        reqConfirmed.changeStatus(RequestStatus.COMPLETED);
        reqConfirmed.changeStatus(RequestStatus.CONFIRMED);
        reliefRequestRepository.save(reqConfirmed);

        // Tạo 1 CANCELLED
        ReliefRequest reqCancelled = createRequest(victim);
        reqCancelled.changeStatus(RequestStatus.CANCELLED);
        reliefRequestRepository.save(reqCancelled);

        AdminService.AdminStats stats = adminService.thongKe();
        assertThat(stats.tong()).isGreaterThanOrEqualTo(2);
        assertThat(stats.daXacNhan()).isGreaterThanOrEqualTo(1);
        assertThat(stats.daHuy()).isGreaterThanOrEqualTo(1);
        assertThat(stats.soNguoiDaNhanHoTro()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void timKiemYeuCauAdmin_locTheoCacTieuChi() {
        Victim victim = createVictim("victim_filter_test@test.com");
        victim.setFullName("Tran Thi Filter");
        victim.setPhoneNumber("0988776655");
        victimRepository.save(victim);

        // Request 1: Victim thường, FOOD + WATER, CRITICAL, SENT, địa điểm Da Nang
        ReliefRequest r1 = new ReliefRequest();
        r1.setVictim(victim);
        r1.setLocation("Hoa Vang, Da Nang");
        r1.setNeedTypes(java.util.Set.of(NeedType.FOOD, NeedType.WATER));
        r1.setNumberOfPeople(3);
        r1.setUrgencyLevel(UrgencyLevel.CRITICAL);
        reliefRequestRepository.save(r1);

        // Request 2: SOS khách vãng lai, MEDICAL, LOW, SENT, địa điểm Quang Nam
        ReliefRequest r2 = new ReliefRequest();
        r2.setVictim(null);
        r2.setGuestName("Khach SOS Dac Biet");
        r2.setGuestPhone("0912999888");
        r2.setLocation("Tam Ky, Quang Nam");
        r2.setNeedTypes(java.util.Set.of(NeedType.MEDICAL));
        r2.setNumberOfPeople(2);
        r2.setUrgencyLevel(UrgencyLevel.LOW);
        reliefRequestRepository.save(r2);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        // 1. Lọc theo urgencyLevel CRITICAL
        org.springframework.data.domain.Page<ReliefRequest> pUrgency =
                adminService.timKiemYeuCauAdmin(null, UrgencyLevel.CRITICAL, null, null, pageable);
        assertThat(pUrgency.getContent()).anyMatch(r -> r.getId().equals(r1.getId()));
        assertThat(pUrgency.getContent()).noneMatch(r -> r.getId().equals(r2.getId()));

        // 2. Lọc theo needType MEDICAL (phải ra r2, không ra r1)
        org.springframework.data.domain.Page<ReliefRequest> pNeed =
                adminService.timKiemYeuCauAdmin(null, null, NeedType.MEDICAL, null, pageable);
        assertThat(pNeed.getContent()).anyMatch(r -> r.getId().equals(r2.getId()));
        assertThat(pNeed.getContent()).noneMatch(r -> r.getId().equals(r1.getId()));

        // 3. Lọc theo từ khóa tên victim (không phân biệt hoa thường)
        org.springframework.data.domain.Page<ReliefRequest> pKwVictim =
                adminService.timKiemYeuCauAdmin(null, null, null, "thi filter", pageable);
        assertThat(pKwVictim.getContent()).anyMatch(r -> r.getId().equals(r1.getId()));
        assertThat(pKwVictim.getContent()).noneMatch(r -> r.getId().equals(r2.getId()));

        // 4. Lọc theo từ khóa SĐT khách SOS
        org.springframework.data.domain.Page<ReliefRequest> pKwPhone =
                adminService.timKiemYeuCauAdmin(null, null, null, "0912999888", pageable);
        assertThat(pKwPhone.getContent()).anyMatch(r -> r.getId().equals(r2.getId()));
        assertThat(pKwPhone.getContent()).noneMatch(r -> r.getId().equals(r1.getId()));

        // 5. Lọc theo từ khóa địa điểm
        org.springframework.data.domain.Page<ReliefRequest> pKwLocation =
                adminService.timKiemYeuCauAdmin(null, null, null, "Hoa Vang", pageable);
        assertThat(pKwLocation.getContent()).anyMatch(r -> r.getId().equals(r1.getId()));
        assertThat(pKwLocation.getContent()).noneMatch(r -> r.getId().equals(r2.getId()));
    }

    @Test
    void timKiemYeuCauAdmin_thuTuSapXepChuaXongVaKhanCapLenDau() {
        Victim victim = createVictim("victim_sort_test@test.com");

        // Tạo 1 request ĐÃ HỦY (nhóm đã xong/hủy)
        ReliefRequest reqCancelled = createRequest(victim);
        reqCancelled.setLocation("SORT_LOCATION_TEST_UNIQUE");
        reqCancelled.changeStatus(RequestStatus.CANCELLED);
        reliefRequestRepository.save(reqCancelled);

        // Tạo 1 request SENT - mức LOW
        ReliefRequest reqSentLow = createRequest(victim);
        reqSentLow.setLocation("SORT_LOCATION_TEST_UNIQUE");
        reqSentLow.setUrgencyLevel(UrgencyLevel.LOW);
        reliefRequestRepository.save(reqSentLow);

        // Tạo 1 request SENT - mức CRITICAL
        ReliefRequest reqSentCritical = createRequest(victim);
        reqSentCritical.setLocation("SORT_LOCATION_TEST_UNIQUE");
        reqSentCritical.setUrgencyLevel(UrgencyLevel.CRITICAL);
        reliefRequestRepository.save(reqSentCritical);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<ReliefRequest> page =
                adminService.timKiemYeuCauAdmin(null, null, null, "SORT_LOCATION_TEST_UNIQUE", pageable);

        List<ReliefRequest> list = page.getContent();
        // Đảm bảo đúng 3 phần tử và thứ tự: CRITICAL lên đầu, rồi LOW, rồi CANCELLED
        assertThat(list).hasSize(3);
        assertThat(list.get(0).getId()).isEqualTo(reqSentCritical.getId());
        assertThat(list.get(1).getId()).isEqualTo(reqSentLow.getId());
        assertThat(list.get(2).getId()).isEqualTo(reqCancelled.getId());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private com.reliefcoordination.relief_coordination.model.Rescuer createRescuer(String email, boolean enabled) {
        com.reliefcoordination.relief_coordination.model.Rescuer rescuer = new com.reliefcoordination.relief_coordination.model.Rescuer();
        rescuer.setFullName("Nguyen Van Rescuer");
        rescuer.setEmail(email);
        rescuer.setPassword("encoded_secret");
        rescuer.setPhoneNumber("0987654322");
        rescuer.setEnabled(enabled);
        return rescuerRepository.save(rescuer);
    }

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
        request.setNeedTypes(java.util.Set.of(NeedType.FOOD));
        request.setNumberOfPeople(3);
        request.setUrgencyLevel(UrgencyLevel.HIGH);
        request.setDescription("Test request");
        return reliefRequestRepository.save(request);
    }
}
