package com.reliefcoordination.relief_coordination.service;

import com.reliefcoordination.relief_coordination.model.Admin;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.User;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Chưa đăng nhập");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng: " + email));
    }

    public Victim getCurrentVictim(Authentication authentication) {
        return as(getCurrentUser(authentication), Victim.class);
    }

    public Rescuer getCurrentRescuer(Authentication authentication) {
        return as(getCurrentUser(authentication), Rescuer.class);
    }

    public Admin getCurrentAdmin(Authentication authentication) {
        return as(getCurrentUser(authentication), Admin.class);
    }

    private <T extends User> T as(User user, Class<T> type) {
        if (!type.isInstance(user)) {
            throw new IllegalStateException(
                    "Người dùng " + user.getEmail() + " không phải " + type.getSimpleName());
        }
        return type.cast(user);
    }
}