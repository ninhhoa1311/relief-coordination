package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.controller.form.ProfileForm;
import com.reliefcoordination.relief_coordination.model.User;
import com.reliefcoordination.relief_coordination.repository.UserRepository;
import com.reliefcoordination.relief_coordination.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(CurrentUserService currentUserService,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String showProfile(Authentication authentication, Model model) {
        User user = currentUserService.getCurrentUser(authentication);

        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setPhoneNumber(user.getPhoneNumber());

        model.addAttribute("profileForm", form);
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("content", "profile/edit");
        model.addAttribute("pageTitle", "Hồ sơ cá nhân");
        return "fragments/layout";
    }

    @PostMapping
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult result,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        User user = currentUserService.getCurrentUser(authentication);

        // Validate password change if requested
        boolean changingPassword = form.getNewPassword() != null
                && !form.getNewPassword().isBlank();

        if (changingPassword) {
            if (form.getCurrentPassword() == null || form.getCurrentPassword().isBlank()) {
                result.rejectValue("currentPassword", "required",
                        "Vui lòng nhập mật khẩu hiện tại");
            } else if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
                result.rejectValue("currentPassword", "wrong",
                        "Mật khẩu hiện tại không đúng");
            }
            if (form.getNewPassword() != null
                    && !form.getNewPassword().equals(form.getConfirmNewPassword())) {
                result.rejectValue("confirmNewPassword", "mismatch",
                        "Mật khẩu mới nhập lại không khớp");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("content", "profile/edit");
            model.addAttribute("pageTitle", "Hồ sơ cá nhân");
            return "fragments/layout";
        }

        // Apply changes
        user.setFullName(form.getFullName());
        user.setPhoneNumber(form.getPhoneNumber());

        if (changingPassword) {
            user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        }

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công!");
        return "redirect:/profile";
    }
}
