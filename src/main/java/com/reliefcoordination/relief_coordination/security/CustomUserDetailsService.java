package com.reliefcoordination.relief_coordination.security;

import com.reliefcoordination.relief_coordination.model.Admin;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.User;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.AdminRepository;
import com.reliefcoordination.relief_coordination.repository.RescuerRepository;
import com.reliefcoordination.relief_coordination.repository.VictimRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final RescuerRepository rescuerRepository;
    private final VictimRepository victimRepository;

    public CustomUserDetailsService(AdminRepository adminRepository,
                                     RescuerRepository rescuerRepository,
                                     VictimRepository victimRepository) {
        this.adminRepository = adminRepository;
        this.rescuerRepository = rescuerRepository;
        this.victimRepository = victimRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findUserByEmail(email);

        String role;
        if (user instanceof Admin) {
            role = "ROLE_ADMIN";
        } else if (user instanceof Rescuer) {
            role = "ROLE_RESCUER";
        } else {
            role = "ROLE_VICTIM";
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
    }

    private User findUserByEmail(String email) {
        return adminRepository.findAll().stream()
                .filter(a -> a.getEmail().equals(email))
                .findFirst()
                .map(a -> (User) a)
                .or(() -> rescuerRepository.findAll().stream()
                        .filter(r -> r.getEmail().equals(email))
                        .findFirst()
                        .map(r -> (User) r))
                .or(() -> victimRepository.findAll().stream()
                        .filter(v -> v.getEmail().equals(email))
                        .findFirst()
                        .map(v -> (User) v))
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay tai khoan: " + email));
    }
}