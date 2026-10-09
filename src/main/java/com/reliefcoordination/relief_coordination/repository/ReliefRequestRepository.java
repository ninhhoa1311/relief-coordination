package com.reliefcoordination.relief_coordination.repository;

import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReliefRequestRepository extends JpaRepository<ReliefRequest, Long> {
    List<ReliefRequest> findByStatus(RequestStatus status);
    List<ReliefRequest> findByVictim(Victim victim);
    List<ReliefRequest> findByRescuer(Rescuer rescuer);
    long countByStatus(RequestStatus status);
    List<ReliefRequest> findByVictimOrderByCreatedAtDesc(Victim victim);
}