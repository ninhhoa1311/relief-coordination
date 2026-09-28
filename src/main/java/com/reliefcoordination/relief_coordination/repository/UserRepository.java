package com.reliefcoordination.relief_coordination.repository;

import com.reliefcoordination.relief_coordination.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}