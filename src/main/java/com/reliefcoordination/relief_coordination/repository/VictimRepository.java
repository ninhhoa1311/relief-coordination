package com.reliefcoordination.relief_coordination.repository;

import com.reliefcoordination.relief_coordination.model.Victim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VictimRepository extends JpaRepository<Victim, Long> {
}