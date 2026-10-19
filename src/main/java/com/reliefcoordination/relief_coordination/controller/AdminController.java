package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // -----------------------------------------------------------------------
    // Dashboard
    // -----------------------------------------------------------------------

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("stats", adminService.thongKe());
        model.addAttribute("content", "admin/home");
        model.addAttribute("pageTitle", "Trang chủ Admin");
        model.addAttribute("role", "ADMIN");
        return "fragments/layout";
    }

    // -----------------------------------------------------------------------
    // Quản lý tài khoản
    // -----------------------------------------------------------------------

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", adminService.danhSachTaiKhoan());
        model.addAttribute("content", "admin/users");
        model.addAttribute("pageTitle", "Quản lý tài khoản");
        model.addAttribute("role", "ADMIN");
        return "fragments/layout";
    }

    /** Chỉ nhận loai = "victim" hoặc "rescuer"; Service sẽ ném lỗi nếu khác. */
    @PostMapping("/users/{loai}/{id}/toggle")
    public String toggleKhoa(@PathVariable String loai,
                             @PathVariable Long id) {
        adminService.toggleKhoa(loai, id);
        return "redirect:/admin/users";
    }

    // -----------------------------------------------------------------------
    // Quản lý yêu cầu
    // -----------------------------------------------------------------------

    @GetMapping("/requests")
    public String requests(Model model) {
        model.addAttribute("requests", adminService.danhSachYeuCau());
        model.addAttribute("content", "admin/requests");
        model.addAttribute("pageTitle", "Quản lý yêu cầu");
        model.addAttribute("role", "ADMIN");
        return "fragments/layout";
    }

    @PostMapping("/requests/{id}/cancel")
    public String cancelRequest(@PathVariable Long id) {
        adminService.huyYeuCau(id);
        return "redirect:/admin/requests";
    }
}