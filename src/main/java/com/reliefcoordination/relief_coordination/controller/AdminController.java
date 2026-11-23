package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // -----------------------------------------------------------------------
    // Dashboard & Quản lý yêu cầu (Hợp nhất)
    // -----------------------------------------------------------------------

    @GetMapping("/home")
    public String home(
            @RequestParam(value = "status", required = false) String statusStr,
            @RequestParam(value = "urgencyLevel", required = false) String urgencyStr,
            @RequestParam(value = "needType", required = false) String needTypeStr,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        com.reliefcoordination.relief_coordination.model.RequestStatus status =
                parseEnum(com.reliefcoordination.relief_coordination.model.RequestStatus.class, statusStr);
        com.reliefcoordination.relief_coordination.model.UrgencyLevel urgencyLevel =
                parseEnum(com.reliefcoordination.relief_coordination.model.UrgencyLevel.class, urgencyStr);
        com.reliefcoordination.relief_coordination.model.NeedType needType =
                parseEnum(com.reliefcoordination.relief_coordination.model.NeedType.class, needTypeStr);

        int pageNumber = Math.max(0, page);
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(pageNumber, 10);

        org.springframework.data.domain.Page<com.reliefcoordination.relief_coordination.model.ReliefRequest> requestsPage =
                adminService.timKiemYeuCauAdmin(status, urgencyLevel, needType, keyword, pageable);

        model.addAttribute("stats", adminService.thongKe());
        model.addAttribute("requestsPage", requestsPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedUrgency", urgencyLevel);
        model.addAttribute("selectedNeedType", needType);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");

        model.addAttribute("statuses", com.reliefcoordination.relief_coordination.model.RequestStatus.values());
        model.addAttribute("urgencyLevels", com.reliefcoordination.relief_coordination.model.UrgencyLevel.values());
        model.addAttribute("needTypes", com.reliefcoordination.relief_coordination.model.NeedType.values());

        model.addAttribute("content", "admin/home");
        model.addAttribute("pageTitle", "Yêu cầu cứu trợ");
        model.addAttribute("role", "ADMIN");
        return "fragments/layout";
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Quản lý tài khoản
    // -----------------------------------------------------------------------

    @GetMapping("/users")
    public String users(@RequestParam(value = "tab", defaultValue = "victim") String tab,
                        Model model) {
        String currentTab = "rescuer".equalsIgnoreCase(tab) ? "rescuer" : "victim";
        model.addAttribute("victims", adminService.danhSachVictim());
        model.addAttribute("rescuers", adminService.danhSachRescuer());
        model.addAttribute("pendingRescuerCount", adminService.countRescuerChoDuyet());
        model.addAttribute("currentTab", currentTab);
        model.addAttribute("users", adminService.danhSachTaiKhoan());
        model.addAttribute("content", "admin/users");
        model.addAttribute("pageTitle", "Quản lý tài khoản");
        model.addAttribute("role", "ADMIN");
        return "fragments/layout";
    }

    /**
     * Xem chi tiết 1 Victim: thông tin cá nhân và lịch sử yêu cầu của Victim đó.
     * Ném ResourceNotFoundException (-> 404) nếu không tồn tại hoặc không phải Victim.
     */
    @GetMapping("/users/{id}")
    public String chiTietVictim(
            @PathVariable Long id,
            @RequestParam(value = "status", required = false) com.reliefcoordination.relief_coordination.model.RequestStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {
        com.reliefcoordination.relief_coordination.model.Victim victim = adminService.timVictimTheoId(id);
        AdminService.VictimRequestStats stats = adminService.thongKeYeuCauCuaVictim(id);

        int pageNumber = Math.max(0, page);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageNumber, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );
        org.springframework.data.domain.Page<com.reliefcoordination.relief_coordination.model.ReliefRequest> requestsPage =
                adminService.danhSachYeuCauCuaVictim(id, status, pageable);

        model.addAttribute("victim", victim);
        model.addAttribute("stats", stats);
        model.addAttribute("requestsPage", requestsPage);
        model.addAttribute("currentStatus", status);
        model.addAttribute("statuses", com.reliefcoordination.relief_coordination.model.RequestStatus.values());
        model.addAttribute("content", "admin/victim-detail");
        model.addAttribute("pageTitle", "Chi tiết nạn nhân - " + victim.getFullName());
        model.addAttribute("role", "ADMIN");
        return "fragments/layout";
    }

    /** Chỉ nhận loai = "victim" hoặc "rescuer"; Service sẽ ném lỗi nếu khác. */
    @PostMapping("/users/{loai}/{id}/toggle")
    public String toggleKhoa(@PathVariable String loai,
                             @PathVariable Long id,
                             @RequestParam(value = "redirect", required = false) String redirectUrl) {
        adminService.toggleKhoa(loai, id);
        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
            return "redirect:" + redirectUrl;
        }
        String targetTab = "rescuer".equalsIgnoreCase(loai) ? "rescuer" : "victim";
        return "redirect:/admin/users?tab=" + targetTab;
    }

    @PostMapping("/rescuers/{id}/approve")
    public String approveRescuer(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        adminService.duyetRescuer(id);
        redirectAttributes.addFlashAttribute("success", "Đã duyệt tài khoản Rescuer #" + id);
        return "redirect:/admin/users?tab=rescuer";
    }

    @PostMapping("/rescuers/approve-all")
    public String approveAllRescuers(RedirectAttributes redirectAttributes) {
        int count = adminService.duyetTatCaRescuer();
        redirectAttributes.addFlashAttribute("success", "Đã duyệt " + count + " tài khoản Rescuer");
        return "redirect:/admin/users?tab=rescuer";
    }

    // -----------------------------------------------------------------------
    // Quản lý yêu cầu (Route cũ chuyển tiếp về /admin/home)
    // -----------------------------------------------------------------------

    @GetMapping("/requests")
    public String requests(jakarta.servlet.http.HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            return "redirect:/admin/home?" + queryString;
        }
        return "redirect:/admin/home";
    }

    @PostMapping("/requests/{id}/cancel")
    public String cancelRequest(
            @PathVariable Long id,
            @RequestParam(value = "redirect", required = false) String redirectUrl,
            RedirectAttributes redirectAttributes) {
        adminService.huyYeuCau(id);
        redirectAttributes.addFlashAttribute("success", "Đã hủy yêu cầu #" + id + " thành công.");
        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/admin/home";
    }
}