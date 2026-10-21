package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.model.ReliefRequest;
import com.reliefcoordination.relief_coordination.service.RescuerTaskService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rescuer")
public class RescuerTaskController {

    private final RescuerTaskService taskService;

    public RescuerTaskController(RescuerTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    public String myTasks(Authentication authentication, Model model) {
        model.addAttribute("tasks", taskService.getMyTasks(authentication.getName()));
        return render(model, "rescuer/tasks", "Nhiệm vụ của tôi");
    }

    @GetMapping("/tasks/{id}")
    public String taskDetail(@PathVariable Long id, Authentication authentication, Model model) {
        ReliefRequest task = taskService.getMyTask(id, authentication.getName());
        model.addAttribute("task", task);
        model.addAttribute("hasNextStep", taskService.nextStatus(task.getStatus()).isPresent());
        return render(model, "rescuer/task-detail", "Nhiệm vụ #" + task.getId());
    }

    /** Chỉ nhận id: trạng thái đích do Service tự tính, không lấy từ form. */
    @PostMapping("/tasks/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            ReliefRequest task = taskService.advanceStatus(id, authentication.getName());
            redirectAttributes.addFlashAttribute("message",
                    "Đã cập nhật nhiệm vụ #" + task.getId() + " sang trạng thái " + task.getStatus());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Nhiệm vụ vừa được thay đổi ở nơi khác, vui lòng kiểm tra lại trạng thái");
        }
        return "redirect:/rescuer/tasks/" + id;
    }

    private String render(Model model, String content, String pageTitle) {
        model.addAttribute("content", content);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("role", "RESCUER");
        return "fragments/layout";
    }
}