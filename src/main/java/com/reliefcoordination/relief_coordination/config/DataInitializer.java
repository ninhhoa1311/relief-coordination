package com.reliefcoordination.relief_coordination.config;

import com.reliefcoordination.relief_coordination.model.*;
import com.reliefcoordination.relief_coordination.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VictimRepository victimRepository;
    private final RescuerRepository rescuerRepository;
    private final AdminRepository adminRepository;
    private final ReliefRequestRepository reliefRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                            VictimRepository victimRepository,
                            RescuerRepository rescuerRepository,
                            AdminRepository adminRepository,
                            ReliefRequestRepository reliefRequestRepository,
                            PasswordEncoder passwordEncoder) {  
        this.userRepository = userRepository;
        this.victimRepository = victimRepository;
        this.rescuerRepository = rescuerRepository;
        this.adminRepository = adminRepository;
        this.reliefRequestRepository = reliefRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        Admin admin = new Admin();
        admin.setFullName("Quan Tri Vien");
        admin.setEmail("admin@relief.vn");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setPhoneNumber("0900000000");
        adminRepository.save(admin);

        Rescuer rescuer1 = new Rescuer();
        rescuer1.setFullName("Nguyen Van Cuu");
        rescuer1.setEmail("rescuer1@relief.vn");
        rescuer1.setPassword(passwordEncoder.encode("rescuer123"));
        rescuer1.setPhoneNumber("0900000001");
        rescuerRepository.save(rescuer1);

        Rescuer rescuer2 = new Rescuer();
        rescuer2.setFullName("Tran Thi Ho");
        rescuer2.setEmail("rescuer2@relief.vn");
        rescuer2.setPassword(passwordEncoder.encode("rescuer123"));
        rescuer2.setPhoneNumber("0900000002");
        rescuerRepository.save(rescuer2);

        Victim victim1 = new Victim();
        victim1.setFullName("Le Van A");
        victim1.setEmail("victim1@relief.vn");
        victim1.setPassword(passwordEncoder.encode("victim123"));
        victim1.setPhoneNumber("0900000011");
        victimRepository.save(victim1);

        Victim victim2 = new Victim();
        victim2.setFullName("Pham Thi B");
        victim2.setEmail("victim2@relief.vn");
        victim2.setPassword(passwordEncoder.encode("victim123"));
        victim2.setPhoneNumber("0900000012");
        victimRepository.save(victim2);

        Victim victim3 = new Victim();
        victim3.setFullName("Hoang Van C");
        victim3.setEmail("victim3@relief.vn");
        victim3.setPassword(passwordEncoder.encode("victim123"));
        victim3.setPhoneNumber("0900000013");
        victimRepository.save(victim3);

        List<Victim> victims = List.of(victim1, victim2, victim3);
        NeedType[] needTypes = NeedType.values();
        UrgencyLevel[] urgencyLevels = UrgencyLevel.values();

        for (int i = 0; i < 20; i++) {
            ReliefRequest request = new ReliefRequest();
            request.setVictim(victims.get(i % victims.size()));
            request.setLocation("Khu vuc " + (i + 1) + ", Quang Nam");
            request.setNeedType(needTypes[i % needTypes.length]);
            request.setNumberOfPeople((i % 5) + 1);
            request.setUrgencyLevel(urgencyLevels[i % urgencyLevels.length]);
            request.setDescription("Yeu cau cuu tro mau so " + (i + 1));

            int step = i % 5; // 0=SENT, 1=ACCEPTED, 2=PROCESSING, 3=COMPLETED, 4=CONFIRMED
            if (step >= 1) {
                request.setRescuer(i % 2 == 0 ? rescuer1 : rescuer2); // gán rescuer TRƯỚC khi ACCEPTED
                request.changeStatus(RequestStatus.ACCEPTED);
            }
            if (step >= 2) request.changeStatus(RequestStatus.PROCESSING);
            if (step >= 3) request.changeStatus(RequestStatus.COMPLETED);
            if (step >= 4) request.changeStatus(RequestStatus.CONFIRMED);

            reliefRequestRepository.save(request);
        }

        System.out.println(">>> Da tao du lieu mau thanh cong <<<");
    }
}