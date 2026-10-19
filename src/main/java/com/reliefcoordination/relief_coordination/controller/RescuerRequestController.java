package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.service.CurrentUserService;
import com.reliefcoordination.relief_coordination.service.RescuerRequestService;
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
public class RescuerRequestController {

    private final RescuerRequestService rescuerRequestService;
    private final CurrentUserService currentUserService;

    public RescuerRequestController(RescuerRequestService rescuerRequestService,
                                    CurrentUserService currentUserService) {
        this.rescuerRequestService = rescuerRequestService;
        this.currentUserService = currentUserService;
    }

    /** GET /rescuer/home — danh sách yêu cầu SENT, sắp theo khẩn cấp giảm dần. */
    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        List<ReliefRequest> requests = rescuerRequestService.danhSachDangCho();
        model.addAttribute("requests", requests);
        model.addAttribute("needTypes", NeedType.values());
        model.addAttribute("urgencyLevels", UrgencyLevel.values());
        model.addAttribute("content", "rescuer/home");
        model.addAttribute("pageTitle", "Danh sách yêu cầu cứu trợ");
        return "fragments/layout";
    }

    /** GET /rescuer/requests/search — tìm kiếm / lọc yêu cầu SENT. */
    @GetMapping("/requests/search")
    public String search(@RequestParam(required = false) String location,
                         @RequestParam(required = false) NeedType needType,
                         @RequestParam(required = false) UrgencyLevel urgencyLevel,
                         Authentication authentication,
                         Model model) {
        List<ReliefRequest> requests = rescuerRequestService.timKiem(location, needType, urgencyLevel);
        model.addAttribute("requests", requests);
        model.addAttribute("needTypes", NeedType.values());
        model.addAttribute("urgencyLevels", UrgencyLevel.values());
        // Giữ lại giá trị filter để hiển thị lại trên form
        model.addAttribute("filterLocation", location);
        model.addAttribute("filterNeedType", needType);
        model.addAttribute("filterUrgencyLevel", urgencyLevel);
        model.addAttribute("content", "rescuer/home");
        model.addAttribute("pageTitle", "Tìm kiếm yêu cầu cứu trợ");
        return "fragments/layout";
    }

    /** GET /rescuer/requests/{id} — xem chi tiết yêu cầu. */
    @GetMapping("/requests/{id}")
    public String requestDetail(@PathVariable("id") Long id,
                                Authentication authentication,
                                Model model) {
        ReliefRequest request = rescuerRequestService.xemChiTiet(id);
        model.addAttribute("request", request);
        model.addAttribute("content", "rescuer/request-detail");
        model.addAttribute("pageTitle", "Chi tiết yêu cầu cứu trợ #" + id);
        return "fragments/layout";
    }

    /** POST /rescuer/requests/{id}/accept — nhận nhiệm vụ. */
    @PostMapping("/requests/{id}/accept")
    public String acceptRequest(@PathVariable("id") Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        Rescuer rescuer = currentUserService.getCurrentRescuer(authentication);
        rescuerRequestService.nhanNhiemVu(id, rescuer);
        redirectAttributes.addFlashAttribute("success",
                "Bạn đã nhận nhiệm vụ #" + id + " thành công!");
        return "redirect:/rescuer/home";
    }
}