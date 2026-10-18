package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("content", "admin/home");
        model.addAttribute("pageTitle", "Thống kê");
        model.addAttribute("stats", adminService.getDashboardStats());
        return "fragments/layout";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("content", "admin/users");
        model.addAttribute("pageTitle", "Tài khoản");
        model.addAttribute("accounts", adminService.listAccounts());
        return "fragments/layout";
    }

    @PostMapping("/users/{id}/toggle-lock")
    public String toggleLock(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean locked = adminService.toggleLock(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    locked ? "Đã khóa tài khoản." : "Đã mở khóa tài khoản.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/requests")
    public String requests(Model model) {
        model.addAttribute("content", "admin/requests");
        model.addAttribute("pageTitle", "Yêu cầu");
        model.addAttribute("requests", adminService.listRequests());
        return "fragments/layout";
    }

    @GetMapping("/requests/{id}")
    public String requestDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ReliefRequest request = adminService.getRequest(id);
            model.addAttribute("content", "admin/request-detail");
            model.addAttribute("pageTitle", "Yêu cầu #" + request.getId());
            model.addAttribute("request", request);
            model.addAttribute("canCancel", adminService.canCancel(request));
            return "fragments/layout";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/requests";
        }
    }

    @PostMapping("/requests/{id}/cancel")
    public String cancelRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.cancelRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy yêu cầu.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/requests";
    }
}
