package com.reliefcoordination.relief_coordination.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorPageController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            try {
                int statusCode = Integer.parseInt(status.toString());
                if (statusCode == 403) {
                    model.addAttribute("pageTitle", "Không có quyền truy cập");
                    model.addAttribute("content", "error/403");
                    return "fragments/layout";
                } else if (statusCode == 404) {
                    model.addAttribute("pageTitle", "Không tìm thấy");
                    model.addAttribute("content", "error/404");
                    return "fragments/layout";
                }
            } catch (NumberFormatException ignored) {
                // Fallthrough to general error
            }
        }

        model.addAttribute("pageTitle", "Đã xảy ra lỗi");
        model.addAttribute("content", "error/error");
        return "fragments/layout";
    }
}
