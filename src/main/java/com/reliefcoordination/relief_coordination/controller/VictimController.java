package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.dto.ReliefRequestForm;
import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.model.Victim;
import com.reliefcoordination.relief_coordination.service.CurrentUserService;
import com.reliefcoordination.relief_coordination.service.VictimRequestService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/victim")
public class VictimController {

    private final VictimRequestService victimRequestService;
    private final CurrentUserService currentUserService;

    public VictimController(VictimRequestService victimRequestService,
                            CurrentUserService currentUserService) {
        this.victimRequestService = victimRequestService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        Victim victim = (Victim) currentUserService.getCurrentUser(authentication);
        List<ReliefRequest> requests = victimRequestService.danhSachCuaToi(victim);
        model.addAttribute("requests", requests);
        model.addAttribute("content", "victim/home");
        model.addAttribute("pageTitle", "Yêu cầu cứu trợ của tôi");
        return "fragments/layout";
    }

    @GetMapping("/requests/new")
    public String newRequestForm(Authentication authentication, Model model) {
        Victim victim = (Victim) currentUserService.getCurrentUser(authentication);
        if (!model.containsAttribute("reliefRequestForm")) {
            model.addAttribute("reliefRequestForm", new ReliefRequestForm());
        }
        model.addAttribute("needTypes", NeedType.values());
        model.addAttribute("urgencyLevels", UrgencyLevel.values());
        model.addAttribute("content", "victim/request-form");
        model.addAttribute("pageTitle", "Tạo yêu cầu cứu trợ");
        model.addAttribute("isEdit", false);
        return "fragments/layout";
    }

    @PostMapping("/requests")
    public String createRequest(@Valid @ModelAttribute("reliefRequestForm") ReliefRequestForm form,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Victim victim = (Victim) currentUserService.getCurrentUser(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("needTypes", NeedType.values());
            model.addAttribute("urgencyLevels", UrgencyLevel.values());
            model.addAttribute("content", "victim/request-form");
            model.addAttribute("pageTitle", "Tạo yêu cầu cứu trợ");
            model.addAttribute("isEdit", false);
            return "fragments/layout";
        }
        victimRequestService.taoYeuCau(victim, form);
        redirectAttributes.addFlashAttribute("success", "Gửi yêu cầu cứu trợ thành công!");
        return "redirect:/victim/home";
    }

    @GetMapping("/requests/{id}")
    public String requestDetail(@PathVariable("id") Long id,
                                Authentication authentication,
                                Model model) {
        Victim victim = (Victim) currentUserService.getCurrentUser(authentication);
        ReliefRequest request = victimRequestService.xemChiTiet(id, victim);
        model.addAttribute("request", request);
        model.addAttribute("content", "victim/request-detail");
        model.addAttribute("pageTitle", "Chi tiết yêu cầu cứu trợ #" + id);
        return "fragments/layout";
    }

    @GetMapping("/requests/{id}/edit")
    public String editRequestForm(@PathVariable("id") Long id,
                                  Authentication authentication,
                                  Model model) {
        Victim victim = (Victim) currentUserService.getCurrentUser(authentication);
        ReliefRequest request = victimRequestService.xemChiTiet(id, victim);
        if (request.getStatus() != RequestStatus.SENT) {
            throw new IllegalStateException("Chỉ được sửa yêu cầu khi trạng thái là SENT");
        }
        if (!model.containsAttribute("reliefRequestForm")) {
            model.addAttribute("reliefRequestForm", ReliefRequestForm.from(request));
        }
        model.addAttribute("requestId", id);
        model.addAttribute("needTypes", NeedType.values());
        model.addAttribute("urgencyLevels", UrgencyLevel.values());
        model.addAttribute("content", "victim/request-form");
        model.addAttribute("pageTitle", "Sửa yêu cầu cứu trợ #" + id);
        model.addAttribute("isEdit", true);
        return "fragments/layout";
    }

    @PostMapping("/requests/{id}")
    public String updateRequest(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("reliefRequestForm") ReliefRequestForm form,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Victim victim = (Victim) currentUserService.getCurrentUser(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("requestId", id);
            model.addAttribute("needTypes", NeedType.values());
            model.addAttribute("urgencyLevels", UrgencyLevel.values());
            model.addAttribute("content", "victim/request-form");
            model.addAttribute("pageTitle", "Sửa yêu cầu cứu trợ #" + id);
            model.addAttribute("isEdit", true);
            return "fragments/layout";
        }
        victimRequestService.suaYeuCau(id, victim, form);
        redirectAttributes.addFlashAttribute("success", "Cập nhật yêu cầu cứu trợ thành công!");
        return "redirect:/victim/requests/" + id;
    }

    @PostMapping("/requests/{id}/cancel")
    public String cancelRequest(@PathVariable("id") Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        Victim victim = (Victim) currentUserService.getCurrentUser(authentication);
        victimRequestService.huyYeuCau(id, victim);
        redirectAttributes.addFlashAttribute("success", "Hủy yêu cầu cứu trợ thành công!");
        return "redirect:/victim/home";
    }

    @PostMapping("/requests/{id}/confirm")
    public String confirmRequest(@PathVariable("id") Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        Victim victim = (Victim) currentUserService.getCurrentUser(authentication);
        victimRequestService.xacNhanDaNhan(id, victim);
        redirectAttributes.addFlashAttribute("success", "Xác nhận nhận cứu trợ thành công!");
        return "redirect:/victim/requests/" + id;
    }
}