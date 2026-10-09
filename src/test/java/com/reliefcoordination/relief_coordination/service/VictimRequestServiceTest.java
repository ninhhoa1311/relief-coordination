package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.dto.VictimRequestForm;
import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import com.reliefcoordination.relief_coordination.repository.RescuerRepository;
import com.reliefcoordination.relief_coordination.repository.VictimRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test nghiệp vụ Victim. @Transactional: tự rollback sau mỗi test. */
@SpringBootTest
@Transactional
class VictimRequestServiceTest {

    @Autowired VictimRequestService service;
    @Autowired VictimRepository victimRepo;
    @Autowired RescuerRepository rescuerRepo;
    @Autowired ReliefRequestRepository requestRepo;

    Victim victimA;
    Victim victimB;

    @BeforeEach
    void setUp() {
        victimA = newVictim("a");
        victimB = newVictim("b");
        loginAs(victimA);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Victim newVictim(String tag) {
        Victim v = new Victim();
        v.setFullName("Victim " + tag);
        v.setEmail(tag + "-" + UUID.randomUUID() + "@test.vn");
        v.setPassword("x");
        v.setPhoneNumber("0900000000");
        return victimRepo.save(v);
    }

    private void loginAs(Victim v) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(v.getEmail(), "x",
                        AuthorityUtils.createAuthorityList("ROLE_VICTIM")));
    }

    private VictimRequestForm form(String location) {
        VictimRequestForm f = new VictimRequestForm();
        f.setLocation(location);
        f.setNeedType(NeedType.FOOD);
        f.setNumberOfPeople(3);
        f.setUrgencyLevel(UrgencyLevel.HIGH);
        f.setDescription("Cần lương thực");
        return f;
    }

    @Test
    void createRequest_startsAsSent_andBelongsToCurrentVictim() {
        ReliefRequest r = service.create(form("Xã A"));
        assertEquals(RequestStatus.SENT, r.getStatus());
        assertEquals(victimA.getId(), r.getVictim().getId());
        assertEquals(1, service.listByVictim().size());
    }

    @Test
    void listByVictim_onlyReturnsOwnRequests() {
        service.create(form("Xã A"));
        loginAs(victimB);
        assertTrue(service.listByVictim().isEmpty());
    }

    @Test
    void otherVictim_cannotViewEditCancelOrConfirm() {
        Long id = service.create(form("Xã A")).getId();
        loginAs(victimB);
        assertThrows(AccessDeniedException.class, () -> service.getDetail(id));
        assertThrows(AccessDeniedException.class, () -> service.update(id, form("Hack")));
        assertThrows(AccessDeniedException.class, () -> service.cancel(id));
        assertThrows(AccessDeniedException.class, () -> service.confirm(id));
    }

    @Test
    void owner_canEditAndCancelWhileSent() {
        Long id = service.create(form("Xã A")).getId();
        service.update(id, form("Xã B"));
        assertEquals("Xã B", service.getDetail(id).getLocation());
        service.cancel(id);
        assertEquals(RequestStatus.CANCELLED, service.getDetail(id).getStatus());
    }

    @Test
    void cannotEditOrCancel_afterAccepted() {
        Long id = service.create(form("Xã A")).getId();
        acceptByRescuer(id);
        assertThrows(IllegalStateException.class, () -> service.update(id, form("Xã B")));
        assertThrows(IllegalStateException.class, () -> service.cancel(id));
    }

    @Test
    void confirm_onlyWhenCompleted() {
        Long id = service.create(form("Xã A")).getId();
        assertThrows(IllegalStateException.class, () -> service.confirm(id));

        ReliefRequest r = acceptByRescuer(id);
        r.changeStatus(RequestStatus.PROCESSING);
        r.changeStatus(RequestStatus.COMPLETED);
        requestRepo.save(r);

        service.confirm(id);
        assertEquals(RequestStatus.CONFIRMED, service.getDetail(id).getStatus());
    }

    @Test
    void detail_exposesRescuerAfterAccepted() {
        Long id = service.create(form("Xã A")).getId();
        ReliefRequest r = acceptByRescuer(id);
        assertEquals(r.getRescuer().getId(), service.getDetail(id).getRescuer().getId());
    }

    private ReliefRequest acceptByRescuer(Long requestId) {
        Rescuer rescuer = new Rescuer();
        rescuer.setFullName("Rescuer");
        rescuer.setEmail("r-" + UUID.randomUUID() + "@test.vn");
        rescuer.setPassword("x");
        rescuer.setPhoneNumber("0911111111");
        rescuerRepo.save(rescuer);

        ReliefRequest r = requestRepo.findById(requestId).orElseThrow();
        r.accept(rescuer);
        return requestRepo.save(r);
    }
}
