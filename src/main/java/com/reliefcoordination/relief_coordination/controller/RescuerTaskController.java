package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.RequestStatus;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.service.CurrentUserService;
import com.reliefcoordination.relief_coordination.service.RescuerTaskService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/rescuer")
public class RescuerTaskController {

    private final RescuerTaskService rescuerTaskService;
    private final CurrentUserService currentUserService;

    public RescuerTaskController(RescuerTaskService rescuerTaskService,
                                 CurrentUserService currentUserService) {
        this.rescuerTaskService = rescuerTaskService;
        this.currentUserService = currentUserService;
    }

    /** GET /rescuer/tasks — danh sách nhiệm vụ của Rescuer, hỗ trợ lọc, sắp xếp và phân trang. */
    @GetMapping("/tasks")
    public String tasks(@RequestParam(required = false) String status,
                        @RequestParam(required = false) String urgencyLevel,
                        @RequestParam(required = false) String needType,
                        @RequestParam(required = false) String location,
                        @RequestParam(required = false) String sort,
                        @RequestParam(defaultValue = "0") int page,
                        Authentication authentication,
                        Model model) {
        Rescuer rescuer = currentUserService.getCurrentRescuer(authentication);
        Page<ReliefRequest> tasksPage = rescuerTaskService.timKiemNhiemVuTheoString(
                rescuer, status, urgencyLevel, needType, location, sort, page);

        model.addAttribute("tasksPage", tasksPage);
        model.addAttribute("tasks", tasksPage.getContent());

        // Danh sách trạng thái nhiệm vụ cho dropdown lọc
        List<RequestStatus> taskStatuses = List.of(
                RequestStatus.ACCEPTED,
                RequestStatus.PROCESSING,
                RequestStatus.COMPLETED,
                RequestStatus.CONFIRMED,
                RequestStatus.CANCELLED
        );
        model.addAttribute("taskStatuses", taskStatuses);
        model.addAttribute("urgencyLevels", UrgencyLevel.values());
        model.addAttribute("needTypes", NeedType.values());

        // Giữ lại giá trị filter & sort
        model.addAttribute("filterStatus", status != null ? status.trim() : "");
        model.addAttribute("filterUrgencyLevel", urgencyLevel != null ? urgencyLevel.trim() : "");
        model.addAttribute("filterNeedType", needType != null ? needType.trim() : "");
        model.addAttribute("filterLocation", location != null ? location.trim() : "");
        model.addAttribute("filterSort", rescuerTaskService.normalizeSortNhiemVu(sort));

        model.addAttribute("content", "rescuer/tasks");
        model.addAttribute("pageTitle", "Nhiệm vụ của tôi");
        return "fragments/layout";
    }

    /**
     * GET /rescuer/tasks/{id} — xem chi tiết nhiệm vụ.
     * Hiện đầy đủ tên + SĐT Victim, địa điểm, link Google Maps (nếu có tọa độ).
     */
    @GetMapping("/tasks/{id}")
    public String taskDetail(@PathVariable("id") Long id,
                             Authentication authentication,
                             Model model) {
        Rescuer rescuer = currentUserService.getCurrentRescuer(authentication);
        ReliefRequest task = rescuerTaskService.xemChiTiet(id, rescuer);
        model.addAttribute("task", task);
        model.addAttribute("content", "rescuer/task-detail");
        model.addAttribute("pageTitle", "Chi tiết nhiệm vụ #" + id);
        return "fragments/layout";
    }

    /**
     * POST /rescuer/tasks/{id}/status — cập nhật sang bước kế tiếp.
     * CHỈ nhận {id} trong URL, KHÔNG nhận tham số trạng thái từ form/body.
     * Server tự tính bước kế tiếp trong RescuerTaskService.
     */
    @PostMapping("/tasks/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Rescuer rescuer = currentUserService.getCurrentRescuer(authentication);
        ReliefRequest updated = rescuerTaskService.capNhatTrangThaiTiep(id, rescuer);
        redirectAttributes.addFlashAttribute("success",
                "Cập nhật trạng thái nhiệm vụ #" + id + " thành công! Trạng thái mới: " + updated.getStatus());
        return "redirect:/rescuer/tasks/" + id;
    }
}