package com.reliefcoordination.relief_coordination.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLock(HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage",
                "Yêu cầu này đã được người khác xử lý, vui lòng tải lại trang.");
        return "redirect:" + backUrl(request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException ex, HttpServletRequest request,
            RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + backUrl(request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(HttpServletResponse response, Model model) {
        response.setStatus(403);
        model.addAttribute("pageTitle", "Không có quyền truy cập");
        model.addAttribute("content", "error/403");
        return "fragments/layout";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(HttpServletResponse response, Model model) {
        response.setStatus(404);
        model.addAttribute("pageTitle", "Không tìm thấy");
        model.addAttribute("content", "error/404");
        return "fragments/layout";
    }

    // Chỉ lấy phần đường dẫn của Referer để tránh redirect sang trang ngoài
    private String backUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                String path = uri.getRawPath();
                if (path != null && path.startsWith("/")) {
                    return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
                }
            } catch (IllegalArgumentException ignored) {
                // Referer không hợp lệ, dùng đường dẫn mặc định bên dưới
            }
        }
        return "/home";
    }
}