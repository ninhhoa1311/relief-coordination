package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.controller.form.RegisterForm;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    // ── Victim registration ─────────────────────────────────────

    @GetMapping("/register/victim")
    public String showRegisterVictim(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register-victim";
    }

    @PostMapping("/register/victim")
    public String registerVictim(@Valid @ModelAttribute("registerForm") RegisterForm form,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        validateRegistration(form, result);
        if (result.hasErrors()) {
            return "auth/register-victim";
        }

        Victim victim = new Victim();
        victim.setFullName(form.getFullName());
        victim.setEmail(form.getEmail());
        victim.setPhoneNumber(form.getPhoneNumber());
        victim.setPassword(passwordEncoder.encode(form.getPassword()));
        victim.setEnabled(true);
        userRepository.save(victim);

        redirectAttributes.addFlashAttribute("success",
                "Đăng ký thành công! Bạn có thể đăng nhập ngay.");
        return "redirect:/login";
    }

    // ── Rescuer registration ────────────────────────────────────

    @GetMapping("/register/rescuer")
    public String showRegisterRescuer(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register-rescuer";
    }

    @PostMapping("/register/rescuer")
    public String registerRescuer(@Valid @ModelAttribute("registerForm") RegisterForm form,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        validateRegistration(form, result);
        if (result.hasErrors()) {
            return "auth/register-rescuer";
        }

        Rescuer rescuer = new Rescuer();
        rescuer.setFullName(form.getFullName());
        rescuer.setEmail(form.getEmail());
        rescuer.setPhoneNumber(form.getPhoneNumber());
        rescuer.setPassword(passwordEncoder.encode(form.getPassword()));
        rescuer.setEnabled(false);
        userRepository.save(rescuer);

        redirectAttributes.addFlashAttribute("success",
                "Đăng ký thành công, tài khoản đang chờ Admin xác minh.");
        return "redirect:/login";
    }

    // ── Shared validation ───────────────────────────────────────

    private void validateRegistration(RegisterForm form, BindingResult result) {
        // Check duplicate email
        if (form.getEmail() != null
                && userRepository.findByEmail(form.getEmail()).isPresent()) {
            result.rejectValue("email", "duplicate", "Email đã được sử dụng");
        }
        // Check password confirmation
        if (form.getPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Mật khẩu nhập lại không khớp");
        }
    }
}