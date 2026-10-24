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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class RescuerRequestServiceTest {

    @Autowired
    private RescuerRequestService rescuerRequestService;

    @Autowired
    private ReliefRequestRepository reliefRequestRepository;

    @Autowired
    private RescuerRepository rescuerRepository;

    @Autowired
    private VictimRepository victimRepository;

    // -----------------------------------------------------------------------
    // Test 1: Race-condition đơn giản — lần 2 phải ném IllegalStateException
    // -----------------------------------------------------------------------

    @Test
    void nhanNhiemVu_lanDau_thanhCong_lanHai_throwsIllegalStateException() {
        // Chuẩn bị: 1 request SENT, 2 rescuer khác nhau
        Victim victim   = createVictim("race_victim@test.com");
        Rescuer rescuerA = createRescuer("rescuerA_race@test.com");
        Rescuer rescuerB = createRescuer("rescuerB_race@test.com");

        ReliefRequest request = createSentRequest(victim, UrgencyLevel.HIGH);

        // Lần 1: Rescuer A nhận — phải thành công
        rescuerRequestService.nhanNhiemVu(request.getId(), rescuerA);

        // Kiểm tra trạng thái đã chuyển sang ACCEPTED
        ReliefRequest reloaded = reliefRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(reloaded.getRescuer().getId()).isEqualTo(rescuerA.getId());

        // Lần 2: Rescuer B cố nhận lại — phải ném IllegalStateException
        // (vì status đã là ACCEPTED, không còn SENT)
        final Long requestId = request.getId();
        assertThrows(IllegalStateException.class, () ->
                rescuerRequestService.nhanNhiemVu(requestId, rescuerB)
        );
    }

    // -----------------------------------------------------------------------
    // Test 2: Sắp xếp theo mức khẩn cấp — CRITICAL phải đứng đầu
    // -----------------------------------------------------------------------

    @Test
    void danhSachDangCho_sapXepKhanCapGiamDan_criticalDungDau() {
        Victim victim = createVictim("sort_victim@test.com");

        // Tạo 4 request với 4 mức khẩn cấp khác nhau (thứ tự insert ngược)
        createSentRequest(victim, UrgencyLevel.LOW);
        createSentRequest(victim, UrgencyLevel.CRITICAL);
        createSentRequest(victim, UrgencyLevel.MEDIUM);
        createSentRequest(victim, UrgencyLevel.HIGH);

        List<ReliefRequest> result = rescuerRequestService.danhSachDangCho();

        // Kết quả phải có ít nhất 4 phần tử (có thể nhiều hơn nếu DB không trống)
        assertThat(result).isNotEmpty();

        // Phần tử đầu tiên phải là CRITICAL
        assertThat(result.get(0).getUrgencyLevel()).isEqualTo(UrgencyLevel.CRITICAL);

        // Toàn bộ danh sách phải theo thứ tự giảm dần: priority[i] >= priority[i+1]
        for (int i = 0; i < result.size() - 1; i++) {
            int priorityCurrent = urgencyPriority(result.get(i).getUrgencyLevel());
            int priorityNext    = urgencyPriority(result.get(i + 1).getUrgencyLevel());
            assertThat(priorityCurrent).isGreaterThanOrEqualTo(priorityNext);
        }
    }

    // -----------------------------------------------------------------------
    // Test 3 (bổ sung): timKiem lọc đúng theo location (case-insensitive)
    // -----------------------------------------------------------------------

    @Test
    void timKiem_theoLocation_khongPhanBietHoaThuong() {
        Victim victim = createVictim("search_victim@test.com");

        createSentRequestWithLocation(victim, UrgencyLevel.MEDIUM, "Hà Nội");
        createSentRequestWithLocation(victim, UrgencyLevel.LOW, "Hồ Chí Minh");

        List<ReliefRequest> result = rescuerRequestService.timKiem("hà nội", null, null);

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(r -> r.getLocation().toLowerCase().contains("hà nội"));
    }

    // -----------------------------------------------------------------------
    // Test 4 (bổ sung): timKiem trả về rỗng khi không khớp bất kỳ request nào
    // -----------------------------------------------------------------------

    @Test
    void timKiem_khongCoKetQua_traVeListRong() {
        Victim victim = createVictim("noresult_victim@test.com");
        createSentRequestWithLocation(victim, UrgencyLevel.LOW, "Đà Nẵng");

        List<ReliefRequest> result = rescuerRequestService.timKiem("XYZ_KHONG_TON_TAI", null, null);

        // Kết quả phải không chứa bất kỳ request nào có location "Đà Nẵng"
        assertThat(result).noneMatch(r -> r.getLocation().equalsIgnoreCase("Đà Nẵng"));
    }

    // -----------------------------------------------------------------------
    // Test 5 (mới): Phân trang & sắp xếp mặc định: khẩn cấp cao -> thấp, cùng mức cũ nhất trước
    // -----------------------------------------------------------------------

    @Test
    void timKiemYeuCauCho_sapXepMacDinh_khanCapCaoXuongThap_cungMucThiCuNhatTruoc() {
        Victim victim = createVictim("sort_default@test.com");

        ReliefRequest req1 = createSentRequestWithCreatedAt(victim, UrgencyLevel.CRITICAL, "Hà Nội", java.time.LocalDateTime.now().minusHours(2));
        ReliefRequest req2 = createSentRequestWithCreatedAt(victim, UrgencyLevel.CRITICAL, "Hà Nội", java.time.LocalDateTime.now().minusHours(1));
        ReliefRequest reqLow = createSentRequestWithCreatedAt(victim, UrgencyLevel.LOW, "Hà Nội", java.time.LocalDateTime.now().minusHours(3));

        org.springframework.data.domain.Page<ReliefRequest> page =
                rescuerRequestService.timKiemYeuCauChoTheoString("Hà Nội", null, null, null, 0);

        assertThat(page.getContent()).isNotEmpty();
        List<Long> ids = page.getContent().stream().map(ReliefRequest::getId).toList();

        // req1 (CRITICAL cũ hơn) phải đứng trước req2 (CRITICAL mới hơn)
        assertThat(ids.indexOf(req1.getId())).isLessThan(ids.indexOf(req2.getId()));
        // req2 (CRITICAL) phải đứng trước reqLow (LOW)
        assertThat(ids.indexOf(req2.getId())).isLessThan(ids.indexOf(reqLow.getId()));
    }

    // -----------------------------------------------------------------------
    // Test 6 (mới): Sắp xếp mức khẩn cấp: thấp -> cao
    // -----------------------------------------------------------------------

    @Test
    void timKiemYeuCauCho_sapXepKhanCapThapLenCao() {
        Victim victim = createVictim("sort_asc@test.com");

        ReliefRequest reqLow = createSentRequestWithCreatedAt(victim, UrgencyLevel.LOW, "Đà Lạt", java.time.LocalDateTime.now().minusHours(2));
        ReliefRequest reqCrit = createSentRequestWithCreatedAt(victim, UrgencyLevel.CRITICAL, "Đà Lạt", java.time.LocalDateTime.now().minusHours(1));

        org.springframework.data.domain.Page<ReliefRequest> page =
                rescuerRequestService.timKiemYeuCauChoTheoString("Đà Lạt", null, null, "urgency_asc", 0);

        List<Long> ids = page.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(ids.indexOf(reqLow.getId())).isLessThan(ids.indexOf(reqCrit.getId()));
    }

    // -----------------------------------------------------------------------
    // Test 7 (mới): Sắp xếp thời gian tạo: mới nhất trước & cũ nhất trước
    // -----------------------------------------------------------------------

    @Test
    void timKiemYeuCauCho_sapXepThoiGianTao() {
        Victim victim = createVictim("sort_time@test.com");

        ReliefRequest reqOld = createSentRequestWithCreatedAt(victim, UrgencyLevel.MEDIUM, "Huế", java.time.LocalDateTime.now().minusHours(5));
        ReliefRequest reqNew = createSentRequestWithCreatedAt(victim, UrgencyLevel.CRITICAL, "Huế", java.time.LocalDateTime.now().minusHours(1));

        // Mới nhất trước: reqNew trước reqOld
        org.springframework.data.domain.Page<ReliefRequest> pageDesc =
                rescuerRequestService.timKiemYeuCauChoTheoString("Huế", null, null, "created_desc", 0);
        List<Long> idsDesc = pageDesc.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(idsDesc.indexOf(reqNew.getId())).isLessThan(idsDesc.indexOf(reqOld.getId()));

        // Cũ nhất trước: reqOld trước reqNew
        org.springframework.data.domain.Page<ReliefRequest> pageAsc =
                rescuerRequestService.timKiemYeuCauChoTheoString("Huế", null, null, "created_asc", 0);
        List<Long> idsAsc = pageAsc.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(idsAsc.indexOf(reqOld.getId())).isLessThan(idsAsc.indexOf(reqNew.getId()));
    }

    // -----------------------------------------------------------------------
    // Test 8 (mới): Lọc theo NeedType
    // -----------------------------------------------------------------------

    @Test
    void timKiemYeuCauCho_locTheoNeedType() {
        Victim victim = createVictim("filter_need@test.com");

        ReliefRequest reqFood = createSentRequestWithNeed(victim, java.util.Set.of(NeedType.FOOD), "Hải Phòng");
        ReliefRequest reqWater = createSentRequestWithNeed(victim, java.util.Set.of(NeedType.WATER), "Hải Phòng");

        org.springframework.data.domain.Page<ReliefRequest> page =
                rescuerRequestService.timKiemYeuCauChoTheoString("Hải Phòng", "WATER", null, "urgency_desc", 0);

        List<Long> ids = page.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(ids).contains(reqWater.getId());
        assertThat(ids).doesNotContain(reqFood.getId());
    }

    // -----------------------------------------------------------------------
    // Test 9 (mới): Yêu cầu SOS của khách vãng lai (victim = null) hoạt động bình thường
    // -----------------------------------------------------------------------

    @Test
    void timKiemYeuCauCho_sosKhach_hoatDongBinhThuong() {
        ReliefRequest guestSos = new ReliefRequest();
        guestSos.setVictim(null);
        guestSos.setGuestName("Nguyen Van Khach");
        guestSos.setGuestPhone("0911222333");
        guestSos.setLocation("Nha Trang");
        guestSos.setNeedTypes(java.util.Set.of(NeedType.RESCUE));
        guestSos.setNumberOfPeople(5);
        guestSos.setUrgencyLevel(UrgencyLevel.CRITICAL);
        guestSos.setDescription("Can ung cuu gap");
        guestSos = reliefRequestRepository.save(guestSos);

        org.springframework.data.domain.Page<ReliefRequest> page =
                rescuerRequestService.timKiemYeuCauChoTheoString("Nha Trang", "RESCUE", "CRITICAL", "urgency_desc", 0);

        List<Long> ids = page.getContent().stream().map(ReliefRequest::getId).toList();
        assertThat(ids).contains(guestSos.getId());
    }

    // -----------------------------------------------------------------------
    // Test 10 (mới): Tham số không hợp lệ fallback về mặc định không lỗi 500
    // -----------------------------------------------------------------------

    @Test
    void timKiemYeuCauCho_thamSoKhongHopLe_fallbackKhongLoi() {
        org.springframework.data.domain.Page<ReliefRequest> page =
                rescuerRequestService.timKiemYeuCauChoTheoString("Hà Nội", "KHONG_TON_TAI", "KHONG_HOP_LE", "sort_bua_bai", -1);

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

    private ReliefRequest createSentRequest(Victim victim, UrgencyLevel urgencyLevel) {
        return createSentRequestWithLocation(victim, urgencyLevel, "123 Duong Le Loi");
    }

    private ReliefRequest createSentRequestWithLocation(Victim victim,
                                                         UrgencyLevel urgencyLevel,
                                                         String location) {
        ReliefRequest request = new ReliefRequest();
        request.setVictim(victim);
        request.setLocation(location);
        request.setNeedTypes(java.util.Set.of(NeedType.FOOD));
        request.setNumberOfPeople(4);
        request.setUrgencyLevel(urgencyLevel);
        request.setDescription("Can ho tro khan cap");
        return reliefRequestRepository.save(request);
    }

    private ReliefRequest createSentRequestWithCreatedAt(Victim victim,
                                                         UrgencyLevel urgencyLevel,
                                                         String location,
                                                         java.time.LocalDateTime createdAt) {
        ReliefRequest request = new ReliefRequest();
        request.setVictim(victim);
        request.setLocation(location);
        request.setNeedTypes(java.util.Set.of(NeedType.FOOD));
        request.setNumberOfPeople(4);
        request.setUrgencyLevel(urgencyLevel);
        request.setDescription("Can ho tro khan cap");
        request.setCreatedAt(createdAt);
        return reliefRequestRepository.save(request);
    }

    private ReliefRequest createSentRequestWithNeed(Victim victim,
                                                    java.util.Set<NeedType> needTypes,
                                                    String location) {
        ReliefRequest request = new ReliefRequest();
        request.setVictim(victim);
        request.setLocation(location);
        request.setNeedTypes(needTypes);
        request.setNumberOfPeople(4);
        request.setUrgencyLevel(UrgencyLevel.HIGH);
        request.setDescription("Can ho tro khan cap");
        return reliefRequestRepository.save(request);
    }

    /** Map urgencyLevel sang số ưu tiên, nhất quán với RescuerRequestService. */
    private int urgencyPriority(UrgencyLevel level) {
        return switch (level) {
            case CRITICAL -> 3;
            case HIGH     -> 2;
            case MEDIUM   -> 1;
            case LOW      -> 0;
        };
    }
}
