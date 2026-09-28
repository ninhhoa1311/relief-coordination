package com.reliefcoordination.relief_coordination.repository;

import com.reliefcoordination.relief_coordination.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
}