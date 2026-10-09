package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.dto.VictimRequestForm;
import com.reliefcoordination.relief_coordination.model.NeedType;
import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.model.UrgencyLevel;
import com.reliefcoordination.relief_coordination.service.VictimRequestService;
import jakarta.validation.Valid;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/victim")
public class VictimController {

    private static final String LAYOUT = "fragments/layout";

    private final VictimRequestService requestService;

    public VictimController(VictimRequestService requestService) {
        this.requestService = requestService;
    }

    /** Danh sách yêu cầu của tôi. */
    @GetMapping("/home")
    public String home(Model model, Principal principal) {
        model.addAttribute("requests", requestService.listByVictim());
        return page(model, "victim/home", "Yêu cầu của tôi");
    }

    /** Form tạo yêu cầu. */
    @GetMapping("/requests/new")
    public String newForm(Model model) {
        model.addAttribute("form", new VictimRequestForm());
        model.addAttribute("formAction", "/victim/requests");
        model.addAttribute("formTitle", "Tạo yêu cầu cứu trợ");
        return page(model, "victim/request-form", "Tạo yêu cầu");
    }

    @PostMapping("/requests")
    public String create(@Valid @ModelAttribute("form") VictimRequestForm form,
                         BindingResult bindingResult,
                         Model model,
                         Principal principal,
                         RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/victim/requests");
            model.addAttribute("formTitle", "Tạo yêu cầu cứu trợ");
            return page(model, "victim/request-form", "Tạo yêu cầu");
        }
        ReliefRequest created = requestService.create(form);
        redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu cứu trợ");
        return "redirect:/victim/requests/" + created.getId();
    }

    @GetMapping("/requests/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        model.addAttribute("request", requestService.getDetail(id));
        return page(model, "victim/detail", "Chi tiết yêu cầu #" + id);
    }

    @GetMapping("/requests/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model,
                           Principal principal,
                           RedirectAttributes redirect) {
        ReliefRequest request = requestService.getDetail(id);
        if (!request.isEditable()) {
            redirect.addFlashAttribute("errorMessage",
                    "Chỉ được sửa yêu cầu khi chưa có ai nhận");
            return "redirect:/victim/requests/" + id;
        }
        model.addAttribute("form", VictimRequestForm.from(request));
        return editPage(model, id);
    }

    @PostMapping("/requests/{id}")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("form") VictimRequestForm form,
                       BindingResult bindingResult,
                       Model model,
                       Principal principal,
                       RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            return editPage(model, id);
        }
        try {
            requestService.update(id, form);

            redirect.addFlashAttribute("successMessage", "Đã cập nhật yêu cầu");
        } catch (IllegalStateException | ObjectOptimisticLockingFailureException e) {
            redirect.addFlashAttribute("errorMessage", friendlyMessage(e));
        }
        return "redirect:/victim/requests/" + id;
    }

    /** Hủy yêu cầu (chỉ khi SENT). */
    @PostMapping("/requests/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         Principal principal,
                         RedirectAttributes redirect) {
        try {
            requestService.cancel(id);

            redirect.addFlashAttribute("successMessage", "Đã hủy yêu cầu");
        } catch (IllegalStateException | ObjectOptimisticLockingFailureException e) {
            redirect.addFlashAttribute("errorMessage", friendlyMessage(e));
        }
        return "redirect:/victim/requests/" + id;
    }

    /** Xác nhận đã nhận hỗ trợ (chỉ khi COMPLETED). */
    @PostMapping("/requests/{id}/confirm")
    public String confirm(@PathVariable Long id,
                          Principal principal,
                          RedirectAttributes redirect) {
        try {
            requestService.confirm(id);

            redirect.addFlashAttribute("successMessage", "Đã xác nhận nhận được hỗ trợ");
        } catch (IllegalStateException | ObjectOptimisticLockingFailureException e) {
            redirect.addFlashAttribute("errorMessage", friendlyMessage(e));
        }
        return "redirect:/victim/requests/" + id;
    }

    @ModelAttribute("needTypes")
    public NeedType[] needTypes() {
        return NeedType.values();
    }

    @ModelAttribute("urgencyLevels")
    public UrgencyLevel[] urgencyLevels() {
        return UrgencyLevel.values();
    }

    private String editPage(Model model, Long id) {
        model.addAttribute("formAction", "/victim/requests/" + id);
        model.addAttribute("formTitle", "Sửa yêu cầu #" + id);
        return page(model, "victim/request-form", "Sửa yêu cầu #" + id);
    }

    private String page(Model model, String content, String title) {
        model.addAttribute("content", content);
        model.addAttribute("pageTitle", title);
        model.addAttribute("role", "VICTIM");
        return LAYOUT;
    }

    private String friendlyMessage(RuntimeException e) {
        if (e instanceof ObjectOptimisticLockingFailureException) {
            return "Yêu cầu vừa được cập nhật bởi người khác, vui lòng tải lại trang";
        }
        return e.getMessage();
    }
}