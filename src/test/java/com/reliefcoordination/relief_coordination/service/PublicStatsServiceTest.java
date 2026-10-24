package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.dto.PublicStatsDto;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PublicStatsServiceTest {

    @Autowired
    private PublicStatsService publicStatsService;

    @Autowired
    private ReliefRequestRepository reliefRequestRepository;

    @Test
    void getPublicStats_khongLoi_vaTraVeSoLieuChinhXac() {
        // Lấy số liệu ban đầu
        PublicStatsDto initialStats = publicStatsService.getPublicStats();
        assertThat(initialStats).isNotNull();
        assertThat(initialStats.totalRequests()).isGreaterThanOrEqualTo(0);
        assertThat(initialStats.completedRequests()).isGreaterThanOrEqualTo(0);
        assertThat(initialStats.totalPeopleSupported()).isGreaterThanOrEqualTo(0);

        // Tạo 1 request SENT (5 người)
        ReliefRequest req1 = new ReliefRequest();
        req1.setLocation("Vị trí test 1");
        req1.setUrgencyLevel(UrgencyLevel.HIGH);
        req1.setNumberOfPeople(5);
        reliefRequestRepository.save(req1);

        // Tạo 1 request COMPLETED (4 người)
        ReliefRequest req2 = new ReliefRequest();
        req2.setLocation("Vị trí test 2");
        req2.setUrgencyLevel(UrgencyLevel.CRITICAL);
        req2.setNumberOfPeople(4);
        req2.changeStatus(RequestStatus.ACCEPTED);
        req2.changeStatus(RequestStatus.PROCESSING);
        req2.changeStatus(RequestStatus.COMPLETED);
        reliefRequestRepository.save(req2);

        // Tạo 1 request CONFIRMED (6 người)
        ReliefRequest req3 = new ReliefRequest();
        req3.setLocation("Vị trí test 3");
        req3.setUrgencyLevel(UrgencyLevel.MEDIUM);
        req3.setNumberOfPeople(6);
        req3.changeStatus(RequestStatus.ACCEPTED);
        req3.changeStatus(RequestStatus.PROCESSING);
        req3.changeStatus(RequestStatus.COMPLETED);
        req3.changeStatus(RequestStatus.CONFIRMED);
        reliefRequestRepository.save(req3);

        // Kiểm tra số liệu sau khi thêm
        PublicStatsDto afterStats = publicStatsService.getPublicStats();
        assertThat(afterStats.totalRequests()).isEqualTo(initialStats.totalRequests() + 3);
        assertThat(afterStats.completedRequests()).isEqualTo(initialStats.completedRequests() + 2);
        assertThat(afterStats.totalPeopleSupported()).isEqualTo(initialStats.totalPeopleSupported() + 10);
    }
}
