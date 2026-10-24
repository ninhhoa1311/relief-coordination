package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.Rescuer;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.service.CurrentUserService;
import com.reliefcoordination.relief_coordination.service.RescuerRequestService;
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

    /** GET /rescuer/home — danh sách yêu cầu SENT, hỗ trợ lọc, sắp xếp và phân trang. */
    @GetMapping("/home")
    public String home(@RequestParam(required = false) String location,
                       @RequestParam(required = false) String needType,
                       @RequestParam(required = false) String urgencyLevel,
                       @RequestParam(required = false) String sort,
                       @RequestParam(defaultValue = "0") int page,
                       Authentication authentication,
                       Model model) {
        Page<ReliefRequest> requestsPage = rescuerRequestService.timKiemYeuCauChoTheoString(
                location, needType, urgencyLevel, sort, page);

        model.addAttribute("requestsPage", requestsPage);
        model.addAttribute("requests", requestsPage.getContent());
        model.addAttribute("needTypes", NeedType.values());
        model.addAttribute("urgencyLevels", UrgencyLevel.values());

        // Giữ lại giá trị filter & sort
        model.addAttribute("filterLocation", location != null ? location.trim() : "");
        model.addAttribute("filterNeedType", needType != null ? needType.trim() : "");
        model.addAttribute("filterUrgencyLevel", urgencyLevel != null ? urgencyLevel.trim() : "");
        model.addAttribute("filterSort", rescuerRequestService.normalizeSortCho(sort));

        model.addAttribute("content", "rescuer/home");
        model.addAttribute("pageTitle", "Danh sách yêu cầu cứu trợ");
        return "fragments/layout";
    }

    /** GET /rescuer/requests/search — tìm kiếm / lọc yêu cầu SENT. */
    @GetMapping("/requests/search")
    public String search(@RequestParam(required = false) String location,
                         @RequestParam(required = false) String needType,
                         @RequestParam(required = false) String urgencyLevel,
                         @RequestParam(required = false) String sort,
                         @RequestParam(defaultValue = "0") int page,
                         Authentication authentication,
                         Model model) {
        return home(location, needType, urgencyLevel, sort, page, authentication, model);
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