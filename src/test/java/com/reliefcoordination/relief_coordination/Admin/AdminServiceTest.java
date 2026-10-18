package com.reliefcoordination.relief_coordination;

import com.reliefcoordination.relief_coordination.model.Admin;
import com.reliefcoordination.relief_coordination.repository.ReliefRequestRepository;
import com.reliefcoordination.relief_coordination.repository.UserRepository;
import com.reliefcoordination.relief_coordination.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    ReliefRequestRepository reliefRequestRepository;

    @InjectMocks
    AdminService adminService;

    @Test
    void toggleLock_ShouldFail_WhenAdmin() {
        Admin admin = new Admin();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        assertThrows(IllegalStateException.class, () -> adminService.toggleLock(1L));
    }
}
